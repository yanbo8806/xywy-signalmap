package com.xywy.signalmap.data.importer

import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import org.w3c.dom.Element
import java.io.InputStream
import java.util.Locale
import java.util.UUID

class KmlLayerParser {
    fun parse(
        input: InputStream,
        layerId: String,
    ): ImportParseResult {
        val document = ImportXmlSecurity.newDocumentBuilder().parse(input)
        val placemarks = document.getElementsByTagNameNS("*", "Placemark")
        var skipped = 0
        val records = mutableListOf<SectorRecord>()
        for (index in 0 until placemarks.length) {
            val placemark = placemarks.item(index) as? Element ?: continue
            val record = parsePlacemark(placemark, layerId)
            if (record == null) {
                skipped += 1
            } else {
                records += record
            }
        }
        return ImportParseResult(records, skipped)
    }

    private fun parsePlacemark(
        placemark: Element,
        layerId: String,
    ): SectorRecord? {
        val name = placemark.getElementsByTagNameNS("*", "name").item(0)?.textContent?.trim().orEmpty()
        val description = placemark.getElementsByTagNameNS("*", "description").item(0)?.textContent?.trim().orEmpty()
        val rawFields = extractExtendedData(placemark).toMutableMap()
        if (name.isNotBlank()) rawFields.putIfAbsent("name", name)
        if (description.isNotBlank()) rawFields.putIfAbsent("description", description)

        val polygonCoordinates = extractPolygonCoordinates(placemark)
        val lineCoordinates = if (polygonCoordinates.isEmpty()) extractLineCoordinates(placemark) else emptyList()
        val pointCoordinates = extractPointCoordinates(placemark)
        if (lineCoordinates.isNotEmpty()) {
            rawFields.putIfAbsent("类型", "线")
            rawFields.putIfAbsent("type", "line")
        }
        val centroid = when {
            polygonCoordinates.isNotEmpty() -> polygonCentroid(polygonCoordinates)
            lineCoordinates.isNotEmpty() -> polygonCentroid(lineCoordinates)
            pointCoordinates != null -> pointCoordinates
            else -> null
        } ?: return null

        val canonical = mutableMapOf<String, String>()
        rawFields.forEach { (header, value) ->
            val key = HeaderAliases.canonical(header) ?: return@forEach
            if (value.isBlank()) return@forEach
            canonical.putIfAbsent(key, value)
        }

        val technology = inferTechnology(
            canonical = canonical,
            name = name,
            description = description,
        )

        return SectorRecord(
            id = UUID.randomUUID().toString(),
            layerId = layerId,
            radioTechnology = technology,
            siteId = canonical[HeaderAliases.SITE_ID],
            siteName = canonical[HeaderAliases.SITE_NAME] ?: name.takeIf { it.isNotBlank() },
            cellId = canonical[HeaderAliases.CELL_ID],
            cellName = canonical[HeaderAliases.CELL_NAME] ?: name.takeIf { it.isNotBlank() },
            pci = canonical[HeaderAliases.PCI],
            tacOrLac = canonical[HeaderAliases.TAC],
            band = normalizeBand(canonical[HeaderAliases.BAND], technology, name, description),
            bandwidth = canonical[HeaderAliases.BANDWIDTH],
            vendor = canonical[HeaderAliases.VENDOR],
            operator = canonical[HeaderAliases.OPERATOR],
            province = canonical[HeaderAliases.PROVINCE],
            city = canonical[HeaderAliases.CITY],
            longitude = centroid.longitude,
            latitude = centroid.latitude,
            azimuth = canonical[HeaderAliases.AZIMUTH]?.toDoubleOrNull()?.takeIf(Double::isFinite),
            antennaHeight = canonical[HeaderAliases.ANTENNA_HEIGHT],
            polygonCoordinates = polygonCoordinates.ifEmpty { lineCoordinates },
            rawFields = rawFields,
            isPointOnly = polygonCoordinates.isEmpty() && lineCoordinates.isEmpty() && pointCoordinates != null,
        )
    }

    private fun extractExtendedData(placemark: Element): Map<String, String> {
        val values = mutableMapOf<String, String>()
        val dataNodes = placemark.getElementsByTagNameNS("*", "Data")
        for (index in 0 until dataNodes.length) {
            val data = dataNodes.item(index) as? Element ?: continue
            val name = data.getAttribute("name").trim()
            val value = data.getElementsByTagNameNS("*", "value").item(0)?.textContent?.trim().orEmpty()
            if (name.isNotBlank() && value.isNotBlank()) {
                values[name] = value
            }
        }
        return values
    }

    private fun extractPointCoordinates(placemark: Element): GeoPoint? {
        val pointNode = placemark.getElementsByTagNameNS("*", "Point").item(0) as? Element ?: return null
        val coordinates = pointNode.getElementsByTagNameNS("*", "coordinates").item(0)?.textContent.orEmpty()
        return parseCoordinates(coordinates).firstOrNull()
    }

    private fun extractPolygonCoordinates(placemark: Element): List<GeoPoint> {
        val polygonNode = placemark.getElementsByTagNameNS("*", "Polygon").item(0) as? Element ?: return emptyList()
        val coordinates = polygonNode.getElementsByTagNameNS("*", "coordinates").item(0)?.textContent.orEmpty()
        return parseCoordinates(coordinates).takeIf { it.distinct().size >= 3 }.orEmpty()
    }

    private fun extractLineCoordinates(placemark: Element): List<GeoPoint> {
        val lineNode = placemark.getElementsByTagNameNS("*", "LineString").item(0) as? Element ?: return emptyList()
        val coordinates = lineNode.getElementsByTagNameNS("*", "coordinates").item(0)?.textContent.orEmpty()
        return parseCoordinates(coordinates).takeIf { it.distinct().size >= 2 }.orEmpty()
    }

    private fun parseCoordinates(raw: String): List<GeoPoint> = raw
        .trim()
        .split(Regex("""\s+"""))
        .mapNotNull { token ->
            val parts = token.split(',')
            val longitude = parts.getOrNull(0)?.toDoubleOrNull() ?: return@mapNotNull null
            val latitude = parts.getOrNull(1)?.toDoubleOrNull() ?: return@mapNotNull null
            if (longitude !in -180.0..180.0 || latitude !in -90.0..90.0) return@mapNotNull null
            GeoPoint(longitude = longitude, latitude = latitude)
        }

    private fun polygonCentroid(points: List<GeoPoint>): GeoPoint {
        val uniquePoints = if (points.size > 1 && points.first() == points.last()) points.dropLast(1) else points
        val longitude = uniquePoints.map { it.longitude }.average()
        val latitude = uniquePoints.map { it.latitude }.average()
        return GeoPoint(longitude = longitude, latitude = latitude)
    }

    private fun inferTechnology(
        canonical: Map<String, String>,
        name: String,
        description: String,
    ): RadioTechnology {
        canonical[HeaderAliases.RADIO_TECH]?.let {
            val technology = RadioTechnology.fromRaw(it)
            if (technology != RadioTechnology.UNKNOWN) return technology
        }
        val band = canonical[HeaderAliases.BAND].orEmpty()
        val summary = "$name $description $band ${canonical.values.joinToString(" ")}".uppercase(Locale.US)
        return when {
            "5G" in summary || "NR" in summary || "N28" in summary || "N41" in summary || "N78" in summary || "N79" in summary -> RadioTechnology.NR
            "4G" in summary || "LTE" in summary || Regex("""\bB\d+\b""").containsMatchIn(summary) -> RadioTechnology.LTE
            else -> RadioTechnology.UNKNOWN
        }
    }

    private fun normalizeBand(
        band: String?,
        technology: RadioTechnology,
        name: String,
        description: String,
    ): String? {
        val raw = band?.trim()?.takeIf(String::isNotBlank)
            ?: Regex("""\b(N\d+|B\d+)\b""")
                .find("$name $description".uppercase(Locale.US))
                ?.value
            ?: return null
        val normalized = raw.uppercase(Locale.US)
        return when {
            normalized.startsWith("N") || normalized.startsWith("B") -> normalized
            technology == RadioTechnology.NR && normalized.all(Char::isDigit) -> "N$normalized"
            technology == RadioTechnology.LTE && normalized.all(Char::isDigit) -> "B$normalized"
            else -> normalized
        }
    }
}
