package com.xywy.signalmap.data.importer

import android.content.ContentResolver
import android.net.Uri
import com.xywy.signalmap.domain.geo.ChinaCoordinateTransform
import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.ImportedLayerRenderMode
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.TileCoordinateSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class EngineeringParamImporter(
    private val sheetParser: XlsxSheetParser = XlsxSheetParser(),
    private val kmlLayerParser: KmlLayerParser = KmlLayerParser(),
    private val geoRasterLayerParser: GeoRasterLayerParser = GeoRasterLayerParser(),
) {
    suspend fun previewSheets(
        resolver: ContentResolver,
        uri: Uri,
    ): List<ParsedSheetPreview> = withContext(Dispatchers.IO) {
        resolver.openInputStream(uri)?.use(sheetParser::readSheetPreviews).orEmpty()
    }

    suspend fun parse(
        resolver: ContentResolver,
        uri: Uri,
        displayName: String,
        mimeType: String? = null,
        layerId: String,
        options: ImportOptions = ImportOptions(),
    ): ImportParseResult = withContext(Dispatchers.IO) {
        val format = ImportFileFormatResolver.resolve(
            displayName = displayName,
            mimeType = mimeType,
            sheetConfig = options.sheetConfig,
        )
        resolver.openInputStream(uri)?.use { input ->
            when (format) {
                ImportFileFormat.XLSX -> parseWorkbook(input, layerId, options)
                ImportFileFormat.KML -> applyOptions(kmlLayerParser.parse(input, layerId), options)
                ImportFileFormat.TIFF -> applyOptions(
                    ImportParseResult(
                        records = emptyList(),
                        skippedCount = 0,
                        rasterLayer = geoRasterLayerParser.parseTiff(
                            inputStream = input,
                            displayName = displayName,
                        ),
                    ),
                    options,
                )
                ImportFileFormat.TIFF_ZIP -> applyOptions(
                    ImportParseResult(
                        records = emptyList(),
                        skippedCount = 0,
                        rasterLayer = geoRasterLayerParser.parseZip(
                            inputStream = input,
                            displayName = displayName,
                        ),
                    ),
                    options,
                )
                ImportFileFormat.XYWY -> ImportParseResult(emptyList(), 0)
                ImportFileFormat.UNKNOWN -> ImportParseResult(emptyList(), 0)
            }
        } ?: ImportParseResult(emptyList(), 0)
    }

    internal fun parseSheets(
        sheets: List<ParsedSheet>,
        layerId: String,
        options: ImportOptions = ImportOptions(),
    ): ImportParseResult {
        val selectedSheetName = options.sheetConfig?.selectedSheetName
        val targetSheets = if (selectedSheetName.isNullOrBlank()) {
            sheets
        } else {
            sheets.filter { it.name == selectedSheetName }
        }
        var skippedCount = 0
        val records = mutableListOf<SectorRecord>()
        targetSheets.forEach { sheet ->
            sheet.rows.forEach { row ->
                parseRow(
                    sheetName = sheet.name,
                    row = row,
                    layerId = layerId,
                    options = options,
                )?.let(records::add) ?: run {
                    skippedCount += 1
                }
            }
        }
        return ImportParseResult(records = records, skippedCount = skippedCount)
    }

    private fun parseWorkbook(
        inputStream: java.io.InputStream,
        layerId: String,
        options: ImportOptions,
    ): ImportParseResult {
        var skippedCount = 0
        val records = mutableListOf<SectorRecord>()
        sheetParser.forEachSheetRow(
            inputStream = inputStream,
            selectedSheetName = options.sheetConfig?.selectedSheetName,
            onRow = { sheetName, row ->
                parseRow(
                    sheetName = sheetName,
                    row = row,
                    layerId = layerId,
                    options = options,
                )?.let(records::add) ?: run {
                    skippedCount += 1
                }
            },
        )
        return ImportParseResult(records = records, skippedCount = skippedCount)
    }

    private fun parseRow(
        sheetName: String,
        row: Map<String, String>,
        layerId: String,
        options: ImportOptions,
    ): SectorRecord? {
        val record = mapRowToSector(
            sheetName = sheetName,
            row = row,
            layerId = layerId,
            sheetConfig = options.sheetConfig,
        ) ?: return null
        return if (options.convertGcj02ToWgs84) transformRecord(record) else record
    }

    private fun mapRowToSector(
        sheetName: String,
        row: Map<String, String>,
        layerId: String,
        sheetConfig: ImportSheetConfig?,
    ): SectorRecord? {
        val canonical = buildCanonicalRow(row, sheetConfig?.fieldMapping)
        val longitude = canonical[HeaderAliases.LONGITUDE]?.toDoubleOrNull()
        val latitude = canonical[HeaderAliases.LATITUDE]?.toDoubleOrNull()
        if (longitude == null || latitude == null) return null
        if (longitude !in -180.0..180.0 || latitude !in -90.0..90.0) return null

        val explicitTechnology = canonical[HeaderAliases.RADIO_TECH]
            ?.let(RadioTechnology::fromRaw)
            ?.takeIf { it != RadioTechnology.UNKNOWN }
        val sheetTechnology = RadioTechnology.fromRaw(sheetName)
            .takeIf { it != RadioTechnology.UNKNOWN }
        val technology = explicitTechnology ?: sheetTechnology ?: RadioTechnology.UNKNOWN
        val pointOnly = when (sheetConfig?.renderMode ?: ImportedLayerRenderMode.AUTO) {
            ImportedLayerRenderMode.POINT -> true
            ImportedLayerRenderMode.SECTOR -> false
            ImportedLayerRenderMode.USER -> true
            ImportedLayerRenderMode.AUTO -> !hasEngineeringSignature(canonical)
        }
        val resolvedSiteName = canonical[HeaderAliases.SITE_NAME]
            ?: canonical[HeaderAliases.CELL_NAME]
            ?: canonical[HeaderAliases.SITE_ID]
            ?: canonical[HeaderAliases.CELL_ID]
        val azimuth = canonical[HeaderAliases.AZIMUTH]?.toDoubleOrNull()
            ?: 0.0.takeIf { sheetConfig?.renderMode == ImportedLayerRenderMode.SECTOR }

        return SectorRecord(
            id = UUID.randomUUID().toString(),
            layerId = layerId,
            radioTechnology = technology,
            siteId = canonical[HeaderAliases.SITE_ID],
            siteName = resolvedSiteName,
            cellId = canonical[HeaderAliases.CELL_ID],
            cellName = canonical[HeaderAliases.CELL_NAME],
            pci = canonical[HeaderAliases.PCI],
            tacOrLac = canonical[HeaderAliases.TAC],
            channelNumber = normalizeChannelNumber(canonical[HeaderAliases.CHANNEL_NUMBER]),
            band = normalizeBand(canonical[HeaderAliases.BAND], technology),
            bandwidth = normalizeBandwidth(canonical[HeaderAliases.BANDWIDTH]),
            vendor = canonical[HeaderAliases.VENDOR],
            operator = canonical[HeaderAliases.OPERATOR],
            province = canonical[HeaderAliases.PROVINCE],
            city = canonical[HeaderAliases.CITY],
            longitude = longitude,
            latitude = latitude,
            azimuth = azimuth,
            antennaHeight = canonical[HeaderAliases.ANTENNA_HEIGHT],
            rawFields = selectDetailFields(
                row = row,
                detailHeaders = sheetConfig?.detailHeaders.orEmpty(),
            ),
            isPointOnly = pointOnly,
        )
    }

    private fun selectDetailFields(
        row: Map<String, String>,
        detailHeaders: List<String>,
    ): Map<String, String> {
        val selectedHeaders = linkedSetOf<String>()
        selectedHeaders += detailHeaders.filter { it.isNotBlank() }
        row.keys.filterTo(selectedHeaders) { header ->
            header.contains("beam", ignoreCase = true) || header.contains("娉㈡潫")
        }
        if (selectedHeaders.isEmpty()) return emptyMap()
        return buildMap {
            selectedHeaders.forEach { header ->
                row[header]?.trim()?.takeIf(String::isNotBlank)?.let { put(header, it) }
            }
        }
    }

    private fun buildCanonicalRow(
        row: Map<String, String>,
        fieldMapping: ImportFieldMapping?,
    ): Map<String, String> {
        val canonical = mutableMapOf<String, String>()
        val cellIdentityHeaderByCanonicalKey = mutableMapOf<String, String>()
        row.forEach { (header, value) ->
            val key = HeaderAliases.canonical(header) ?: return@forEach
            if (value.isBlank()) return@forEach
            val trimmed = value.trim()
            if (key == HeaderAliases.CELL_ID) {
                val currentHeader = cellIdentityHeaderByCanonicalKey[key]
                if (currentHeader == null ||
                    HeaderAliases.cellIdentityPriority(header) > HeaderAliases.cellIdentityPriority(currentHeader)
                ) {
                    canonical[key] = trimmed
                    cellIdentityHeaderByCanonicalKey[key] = header
                }
            } else {
                canonical.putIfAbsent(key, trimmed)
            }
        }
        fieldMapping?.toCanonicalHeaderMap().orEmpty().forEach { (canonicalKey, header) ->
            row[header]?.takeIf { it.isNotBlank() }?.trim()?.let { value ->
                if (canonicalKey == HeaderAliases.CELL_ID) {
                    val currentHeader = cellIdentityHeaderByCanonicalKey[canonicalKey]
                    if (currentHeader == null ||
                        HeaderAliases.cellIdentityPriority(header) >= HeaderAliases.cellIdentityPriority(currentHeader)
                    ) {
                        canonical[canonicalKey] = value
                        cellIdentityHeaderByCanonicalKey[canonicalKey] = header
                    }
                } else {
                    canonical[canonicalKey] = value
                }
            }
        }
        return canonical
    }

    private fun hasEngineeringSignature(canonical: Map<String, String>): Boolean = listOf(
        HeaderAliases.CELL_ID,
        HeaderAliases.PCI,
        HeaderAliases.TAC,
        HeaderAliases.BAND,
        HeaderAliases.BANDWIDTH,
        HeaderAliases.AZIMUTH,
        HeaderAliases.ANTENNA_HEIGHT,
    ).any { key ->
        canonical[key]?.isNotBlank() == true
    }

    internal fun normalizeBand(
        value: String?,
        technology: RadioTechnology,
    ): String? {
        val raw = value?.trim()?.takeIf(String::isNotBlank) ?: return null
        val normalized = raw.uppercase(Locale.US)
        return when {
            normalized.startsWith("N") || normalized.startsWith("B") -> normalized
            technology == RadioTechnology.NR && normalized.all(Char::isDigit) -> "N$normalized"
            technology == RadioTechnology.LTE && normalized.all(Char::isDigit) -> "B$normalized"
            normalized.matches(Regex("""\d+""")) -> "B$normalized"
            else -> normalized
        }
    }

    internal fun normalizeBandwidth(value: String?): String? {
        val raw = value?.trim()?.takeIf(String::isNotBlank) ?: return null
        val normalized = raw.uppercase(Locale.US).replace(" ", "")
        Regex("""^(\d+(?:\.\d+)?)(M|MHZ)$""").matchEntire(normalized)?.let {
            return "${it.groupValues[1]} MHz"
        }
        Regex("""^(\d+(?:\.\d+)?)(K|KHZ)$""").matchEntire(normalized)?.let {
            return "${it.groupValues[1]} kHz"
        }
        return raw
    }

    internal fun normalizeChannelNumber(value: String?): String? {
        val raw = value?.trim()?.takeIf(String::isNotBlank) ?: return null
        raw.toLongOrNull()?.let { return it.toString() }
        raw.toDoubleOrNull()?.let { numeric ->
            val rounded = numeric.toLong()
            if (kotlin.math.abs(numeric - rounded.toDouble()) < 0.0001) {
                return rounded.toString()
            }
        }
        return raw
    }

    private fun applyOptions(
        result: ImportParseResult,
        options: ImportOptions,
    ): ImportParseResult {
        if (!options.convertGcj02ToWgs84) return result
        return result.copy(
            records = result.records.map(::transformRecord),
            rasterLayer = result.rasterLayer?.let { raster ->
                if (raster.coordinateSystem == TileCoordinateSystem.GCJ02) {
                    transformRasterLayer(raster).copy(coordinateSystem = TileCoordinateSystem.WGS84)
                } else {
                    raster
                }
            },
        )
    }

    private fun transformRecord(record: SectorRecord): SectorRecord {
        val point = ChinaCoordinateTransform.gcj02ToWgs84(record.longitude, record.latitude)
        return record.copy(
            longitude = point.longitude,
            latitude = point.latitude,
            polygonCoordinates = record.polygonCoordinates.map(::transformPoint),
        )
    }

    private fun transformPoint(point: GeoPoint): GeoPoint =
        ChinaCoordinateTransform.gcj02ToWgs84(point.longitude, point.latitude)

    private fun transformRasterLayer(layer: ParsedRasterLayer): ParsedRasterLayer =
        layer.copy(
            bounds = GeoImageBounds(
                topLeft = transformPoint(layer.bounds.topLeft),
                topRight = transformPoint(layer.bounds.topRight),
                bottomRight = transformPoint(layer.bounds.bottomRight),
                bottomLeft = transformPoint(layer.bounds.bottomLeft),
            ),
        )
}

data class ImportParseResult(
    val records: List<SectorRecord>,
    val skippedCount: Int,
    val rasterLayer: ParsedRasterLayer? = null,
)

data class ImportOptions(
    val convertGcj02ToWgs84: Boolean = false,
    val sheetConfig: ImportSheetConfig? = null,
)
