package com.xywy.signalmap.data.importer

import java.util.Locale

enum class ImportFileFormat {
    XLSX,
    KML,
    TIFF,
    TIFF_ZIP,
    XYWY,
    UNKNOWN,
}

object ImportFileFormatResolver {
    fun resolve(
        displayName: String,
        mimeType: String? = null,
        sheetConfig: ImportSheetConfig? = null,
    ): ImportFileFormat {
        if (sheetConfig != null) return ImportFileFormat.XLSX
        when (mimeType?.lowercase(Locale.US)) {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> return ImportFileFormat.XLSX
            "application/vnd.google-earth.kml+xml" -> return ImportFileFormat.KML
            "image/tiff",
            "image/x-tiff",
            -> return ImportFileFormat.TIFF
            "application/zip",
            "application/x-zip-compressed",
            "multipart/x-zip",
            -> return ImportFileFormat.TIFF_ZIP
            "application/x-xywy-log",
            "application/vnd.xywy.log",
            -> return ImportFileFormat.XYWY
        }
        return when (displayName.substringAfterLast('.', "").lowercase(Locale.US)) {
            "xlsx" -> ImportFileFormat.XLSX
            "kml" -> ImportFileFormat.KML
            "tif", "tiff" -> ImportFileFormat.TIFF
            "zip" -> ImportFileFormat.TIFF_ZIP
            "xywy" -> ImportFileFormat.XYWY
            else -> ImportFileFormat.UNKNOWN
        }
    }
}
