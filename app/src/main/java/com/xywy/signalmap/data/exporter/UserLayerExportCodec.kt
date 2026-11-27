package com.xywy.signalmap.data.exporter

import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.LayerDocument
import com.xywy.signalmap.domain.model.SectorRecord
import java.util.Locale

object UserLayerExportCodec {
    fun toCsv(document: LayerDocument): String = buildString {
        appendLine("name,type,longitude,latitude,icon,polygon,remark")
        document.sectors.forEach { record ->
            appendCsvCell(record.displayName())
            append(',')
            appendCsvCell(record.exportType())
            append(',')
            append(record.longitude.formatCoordinate())
            append(',')
            append(record.latitude.formatCoordinate())
            append(',')
            appendCsvCell(record.rawFields["icon"].orEmpty())
            append(',')
            appendCsvCell(record.polygonCoordinates.joinToString(";") { point ->
                "${point.longitude.formatCoordinate()} ${point.latitude.formatCoordinate()}"
            })
            append(',')
            appendCsvCell(record.rawFields["备注"] ?: record.rawFields["remark"].orEmpty())
            appendLine()
        }
    }

    fun toKml(document: LayerDocument): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<kml xmlns="http://www.opengis.net/kml/2.2">""")
        appendLine("<Document>")
        appendLine("<name>${document.meta.name.escapeXml()}</name>")
        document.sectors.forEach { record ->
            appendLine("<Placemark>")
            appendLine("<name>${record.displayName().escapeXml()}</name>")
            val description = record.rawFields.entries
                .joinToString("<br/>") { "${it.key.escapeXml()}: ${it.value.escapeXml()}" }
            if (description.isNotBlank()) {
                appendLine("<description><![CDATA[$description]]></description>")
            }
            if (record.isPointOnly || record.polygonCoordinates.isEmpty()) {
                appendLine("<Point><coordinates>${record.longitude.formatCoordinate()},${record.latitude.formatCoordinate()},0</coordinates></Point>")
            } else if (record.isLineOnly) {
                appendLine("<LineString><coordinates>")
                appendLine(
                    record.polygonCoordinates.joinToString(" ") { point ->
                        "${point.longitude.formatCoordinate()},${point.latitude.formatCoordinate()},0"
                    },
                )
                appendLine("</coordinates></LineString>")
            } else {
                val ring = closedRing(record.polygonCoordinates)
                appendLine("<Polygon><outerBoundaryIs><LinearRing><coordinates>")
                appendLine(
                    ring.joinToString(" ") { point ->
                        "${point.longitude.formatCoordinate()},${point.latitude.formatCoordinate()},0"
                    },
                )
                appendLine("</coordinates></LinearRing></outerBoundaryIs></Polygon>")
            }
            appendLine("</Placemark>")
        }
        appendLine("</Document>")
        appendLine("</kml>")
    }

    private fun SectorRecord.displayName(): String =
        cellName ?: siteName ?: rawFields["名称"] ?: rawFields["name"] ?: id

    private fun SectorRecord.exportType(): String = when {
        isPointOnly -> "point"
        isLineOnly -> "line"
        else -> "polygon"
    }

    private fun closedRing(points: List<GeoPoint>): List<GeoPoint> =
        if (points.isEmpty() || points.first() == points.last()) points else points + points.first()

    private fun StringBuilder.appendCsvCell(value: String) {
        append('"')
        append(value.replace("\"", "\"\""))
        append('"')
    }

    private fun Double.formatCoordinate(): String = String.format(Locale.US, "%.8f", this)

    private fun String.escapeXml(): String = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
