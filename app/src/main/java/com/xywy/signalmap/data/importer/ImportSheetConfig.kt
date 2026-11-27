package com.xywy.signalmap.data.importer

import com.xywy.signalmap.domain.model.ImportedLayerRenderMode

data class ParsedSheetPreview(
    val name: String,
    val headers: List<String>,
)

data class ImportFieldMapping(
    val longitudeHeader: String? = null,
    val latitudeHeader: String? = null,
    val azimuthHeader: String? = null,
    val nameHeader: String? = null,
    val siteIdHeader: String? = null,
    val cellIdHeader: String? = null,
    val pciHeader: String? = null,
    val antennaHeightHeader: String? = null,
) {
    fun toCanonicalHeaderMap(): Map<String, String> = buildMap {
        longitudeHeader?.let { put(HeaderAliases.LONGITUDE, it) }
        latitudeHeader?.let { put(HeaderAliases.LATITUDE, it) }
        azimuthHeader?.let { put(HeaderAliases.AZIMUTH, it) }
        nameHeader?.let { put(HeaderAliases.SITE_NAME, it) }
        siteIdHeader?.let { put(HeaderAliases.SITE_ID, it) }
        cellIdHeader?.let { put(HeaderAliases.CELL_ID, it) }
        pciHeader?.let { put(HeaderAliases.PCI, it) }
        antennaHeightHeader?.let { put(HeaderAliases.ANTENNA_HEIGHT, it) }
    }
}

data class ImportSheetConfig(
    val selectedSheetName: String,
    val renderMode: ImportedLayerRenderMode = ImportedLayerRenderMode.AUTO,
    val fieldMapping: ImportFieldMapping = ImportFieldMapping(),
    val detailHeaders: List<String> = emptyList(),
)

data class ImportFieldSuggestion(
    val mapping: ImportFieldMapping,
    val defaultDetailHeaders: List<String>,
)
