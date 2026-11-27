package com.xywy.signalmap.data.importer

import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.TileCoordinateSystem
import mil.nga.tiff.FieldTagType
import mil.nga.tiff.FileDirectory
import mil.nga.tiff.Rasters
import mil.nga.tiff.TiffReader
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class ParsedRasterLayer(
    val width: Int,
    val height: Int,
    val alphaMask: ByteArray,
    val validPixelCount: Int,
    val bounds: GeoImageBounds,
    val coordinateSystem: TileCoordinateSystem? = null,
)

data class GeoImageBounds(
    val topLeft: GeoPoint,
    val topRight: GeoPoint,
    val bottomRight: GeoPoint,
    val bottomLeft: GeoPoint,
) {
    val minLongitude: Double
        get() = min(min(topLeft.longitude, topRight.longitude), min(bottomRight.longitude, bottomLeft.longitude))
    val maxLongitude: Double
        get() = max(max(topLeft.longitude, topRight.longitude), max(bottomRight.longitude, bottomLeft.longitude))
    val minLatitude: Double
        get() = min(min(topLeft.latitude, topRight.latitude), min(bottomRight.latitude, bottomLeft.latitude))
    val maxLatitude: Double
        get() = max(max(topLeft.latitude, topRight.latitude), max(bottomRight.latitude, bottomLeft.latitude))
}

data class RasterCompanionFiles(
    val tfwText: String? = null,
    val prjText: String? = null,
)

class GeoRasterLayerParser {
    fun parseTiff(
        inputStream: InputStream,
        displayName: String,
        sidecars: RasterCompanionFiles = RasterCompanionFiles(),
    ): ParsedRasterLayer {
        val bytes = ImportReadBudget(MAX_TIFF_BYTES.toLong()).read(inputStream, MAX_TIFF_BYTES, "TIFF 文件")
        return parseTiffBytes(bytes, displayName, sidecars)
    }

    private fun parseTiffBytes(bytes: ByteArray, displayName: String, sidecars: RasterCompanionFiles): ParsedRasterLayer {
        val image = TiffReader.readTiff(bytes)
        val directory = image.fileDirectory ?: error("TIFF 文件中没有可读取的图像目录：$displayName")
        return parseDirectory(
            directory = directory,
            displayName = displayName,
            sidecars = sidecars,
        )
    }

    fun parseZip(
        inputStream: InputStream,
        displayName: String,
    ): ParsedRasterLayer {
        val entries = linkedMapOf<String, ByteArray>()
        val budget = ImportReadBudget(256L * 1024 * 1024)
        var entryCount = 0
        var tifName: String? = null
        ZipInputStream(inputStream.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(++entryCount <= 1_024) { "栅格压缩包包含过多文件" }
                val entryName = entry.name.replace('\\', '/').lowercase(Locale.US)
                val isTiff = entryName.endsWith(".tif") || entryName.endsWith(".tiff")
                val isSidecar = entryName.endsWith(".tfw") || entryName.endsWith(".prj")
                if (!entry.isDirectory && ((isTiff && tifName == null) || isSidecar)) {
                    require(entryName !in entries) { "压缩包包含重复文件：$entryName" }
                    entries[entryName] = budget.read(zip, if (isTiff) MAX_TIFF_BYTES else 64 * 1024, "栅格文件")
                    if (isTiff) tifName = entryName
                } else {
                    budget.discard(zip)
                }
                zip.closeEntry()
            }
        }
        val tifEntry = entries.entries.firstOrNull { (name, _) ->
            name.endsWith(".tif") || name.endsWith(".tiff")
        } ?: error("ZIP 中未找到 .tif/.tiff 文件：$displayName")
        val baseName = tifEntry.key.substringBeforeLast('.')
        val tfwText = entries["$baseName.tfw"]?.toString(Charsets.UTF_8)
        val prjText = entries["$baseName.prj"]?.toString(Charsets.UTF_8)
        return parseTiffBytes(
            bytes = tifEntry.value,
            displayName = tifEntry.key,
            sidecars = RasterCompanionFiles(
                tfwText = tfwText,
                prjText = prjText,
            ),
        )
    }

    private fun parseDirectory(
        directory: FileDirectory,
        displayName: String,
        sidecars: RasterCompanionFiles,
    ): ParsedRasterLayer {
        val sourceWidth = directory.imageWidth?.toInt() ?: error("无法读取 TIFF 宽度：$displayName")
        val sourceHeight = directory.imageHeight?.toInt() ?: error("无法读取 TIFF 高度：$displayName")
        val pixelCount = sourceWidth.toLong() * sourceHeight.toLong()
        val bytesPerPixel = directory.bitsPerSample.orEmpty().sumOf { (it.toLong() + 7) / 8 }
        require(sourceWidth > 0 && sourceHeight > 0 && pixelCount <= 16_777_216L &&
            bytesPerPixel in 1..128 && pixelCount * bytesPerPixel <= MAX_TIFF_BYTES) {
            "TIFF 栅格过大，请降低分辨率或裁剪后导入：$displayName"
        }
        val bounds = resolveBounds(directory, sidecars)
            ?: error("无法读取地理参考信息，请提供带 GeoTIFF 配准信息的 tif，或使用包含 tif+tfw+prj 的 zip 导入")
        require(listOf(bounds.topLeft, bounds.topRight, bounds.bottomRight, bounds.bottomLeft).all {
            it.longitude in -180.0..180.0 && it.latitude in -90.0..90.0
        }) { "栅格坐标无效或使用投影坐标，请先转换为经纬度坐标后导入" }
        val coordinateSystem = resolveCoordinateSystem(directory, sidecars.prjText)
        val rasters = directory.readRasters()
        val noData = parseNoData(directory)
        val outputSize = resolveOutputSize(sourceWidth, sourceHeight)
        val stats = collectStats(rasters, sourceWidth, sourceHeight, noData)
        require(stats.validPixelCount > 0) { "TIFF 栅格中没有有效像素：$displayName" }
        val alphaMask = buildAlphaMask(
            rasters = rasters,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            outputWidth = outputSize.width,
            outputHeight = outputSize.height,
            noData = noData,
        )
        return ParsedRasterLayer(
            width = outputSize.width,
            height = outputSize.height,
            alphaMask = alphaMask,
            validPixelCount = stats.validPixelCount,
            bounds = bounds,
            coordinateSystem = coordinateSystem,
        )
    }

    private fun resolveBounds(
        directory: FileDirectory,
        sidecars: RasterCompanionFiles,
    ): GeoImageBounds? =
        sidecars.tfwText?.let {
            parseWorldFile(
                text = it,
                width = directory.imageWidth?.toDouble() ?: return null,
                height = directory.imageHeight?.toDouble() ?: return null,
            )
        }
            ?: parseModelTransformation(directory)
            ?: parseModelPixelScaleAndTiePoint(directory)

    private fun parseModelPixelScaleAndTiePoint(directory: FileDirectory): GeoImageBounds? {
        val scale = directory.modelPixelScale ?: return null
        val tiePoint = directory.modelTiepoint ?: return null
        if (scale.size < 2 || tiePoint.size < 6) return null
        val width = directory.imageWidth?.toDouble() ?: return null
        val height = directory.imageHeight?.toDouble() ?: return null
        val scaleX = scale[0]
        val scaleY = scale[1]
        val rasterI = tiePoint[0]
        val rasterJ = tiePoint[1]
        val modelX = tiePoint[3]
        val modelY = tiePoint[4]
        val originX = modelX - (rasterI * scaleX)
        val originY = modelY + (rasterJ * scaleY)
        val topLeft = GeoPoint(longitude = originX, latitude = originY)
        val topRight = GeoPoint(longitude = originX + (width * scaleX), latitude = originY)
        val bottomRight = GeoPoint(longitude = originX + (width * scaleX), latitude = originY - (height * scaleY))
        val bottomLeft = GeoPoint(longitude = originX, latitude = originY - (height * scaleY))
        return GeoImageBounds(topLeft, topRight, bottomRight, bottomLeft)
    }

    private fun parseModelTransformation(directory: FileDirectory): GeoImageBounds? {
        val matrix = directory.getDoubleListEntryValue(FieldTagType.ModelTransformation) ?: return null
        if (matrix.size < 16) return null
        val width = directory.imageWidth?.toDouble() ?: return null
        val height = directory.imageHeight?.toDouble() ?: return null
        fun map(i: Double, j: Double): GeoPoint {
            val x = (matrix[0] * i) + (matrix[1] * j) + matrix[3]
            val y = (matrix[4] * i) + (matrix[5] * j) + matrix[7]
            return GeoPoint(longitude = x, latitude = y)
        }
        return GeoImageBounds(
            topLeft = map(0.0, 0.0),
            topRight = map(width, 0.0),
            bottomRight = map(width, height),
            bottomLeft = map(0.0, height),
        )
    }

    private fun parseWorldFile(
        text: String,
        width: Double,
        height: Double,
    ): GeoImageBounds? {
        val lines = text
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toList()
        if (lines.size < 6) return null
        val values = lines.take(6).mapNotNull(String::toDoubleOrNull)
        if (values.size < 6) return null
        val a = values[0]
        val d = values[1]
        val b = values[2]
        val e = values[3]
        val c = values[4]
        val f = values[5]
        fun map(pixel: Double, row: Double): GeoPoint {
            val x = (a * pixel) + (b * row) + c
            val y = (d * pixel) + (e * row) + f
            return GeoPoint(longitude = x, latitude = y)
        }
        return GeoImageBounds(
            topLeft = map(-0.5, -0.5),
            topRight = map(width - 0.5, -0.5),
            bottomRight = map(width - 0.5, height - 0.5),
            bottomLeft = map(-0.5, height - 0.5),
        )
    }

    private fun resolveCoordinateSystem(
        directory: FileDirectory,
        prjText: String?,
    ): TileCoordinateSystem? =
        parseCoordinateSystem(prjText)
            ?: parseCoordinateSystem(directory.getStringEntryValue(FieldTagType.GeoAsciiParams))
            ?: parseGeoKeyCoordinateSystem(directory.getIntegerListEntryValue(FieldTagType.GeoKeyDirectory))

    private fun parseCoordinateSystem(text: String?): TileCoordinateSystem? {
        val value = text?.uppercase(Locale.US) ?: return null
        return when {
            "GCJ" in value -> TileCoordinateSystem.GCJ02
            "WGS 84" in value || "WGS84" in value || "EPSG:4326" in value -> TileCoordinateSystem.WGS84
            else -> null
        }
    }

    private fun parseGeoKeyCoordinateSystem(geoKeyDirectory: List<Int>?): TileCoordinateSystem? {
        if (geoKeyDirectory == null || geoKeyDirectory.size < 8) return null
        var index = 4
        while (index + 3 < geoKeyDirectory.size) {
            val keyId = geoKeyDirectory[index]
            val tiffTagLocation = geoKeyDirectory[index + 1]
            val count = geoKeyDirectory[index + 2]
            val valueOffset = geoKeyDirectory[index + 3]
            if (count == 1 && tiffTagLocation == 0 && (keyId == 2048 || keyId == 3072) && valueOffset == 4326) {
                return TileCoordinateSystem.WGS84
            }
            index += 4
        }
        return null
    }

    private fun parseNoData(directory: FileDirectory): Double? {
        val raw = directory.getStringEntryValue(FieldTagType.GDAL_NODATA)?.trim() ?: return null
        return when {
            raw.equals("nan", ignoreCase = true) -> Double.NaN
            else -> raw.toDoubleOrNull()
        }
    }

    private fun collectStats(
        rasters: Rasters,
        width: Int,
        height: Int,
        noData: Double?,
    ): RasterStats {
        var validPixelCount = 0
        for (row in 0 until height) {
            for (column in 0 until width) {
                val value = rasters.getFirstPixelSample(column, row)?.toDouble() ?: continue
                if (isNoData(value, noData)) continue
                validPixelCount += 1
            }
        }
        return RasterStats(
            validPixelCount = validPixelCount,
        )
    }

    private fun buildAlphaMask(
        rasters: Rasters,
        sourceWidth: Int,
        sourceHeight: Int,
        outputWidth: Int,
        outputHeight: Int,
        noData: Double?,
    ): ByteArray {
        val scaleX = sourceWidth.toDouble() / outputWidth.toDouble()
        val scaleY = sourceHeight.toDouble() / outputHeight.toDouble()
        return ByteArray(outputWidth * outputHeight).also { mask ->
            for (row in 0 until outputHeight) {
                val sourceRow = min(((row + 0.5) * scaleY).toInt(), sourceHeight - 1)
                for (column in 0 until outputWidth) {
                    val sourceColumn = min(((column + 0.5) * scaleX).toInt(), sourceWidth - 1)
                    val value = rasters.getFirstPixelSample(sourceColumn, sourceRow)?.toDouble()
                    val alpha = when {
                        value == null -> 0
                        isNoData(value, noData) -> 0
                        else -> 255
                    }
                    mask[(row * outputWidth) + column] = alpha.toByte()
                }
            }
        }
    }

    private fun isNoData(
        value: Double,
        noData: Double?,
    ): Boolean {
        if (!value.isFinite()) return true
        if (noData == null) return false
        return if (noData.isNaN()) {
            value.isNaN()
        } else {
            kotlin.math.abs(value - noData) <= 1e-9
        }
    }

    private fun resolveOutputSize(
        width: Int,
        height: Int,
    ): RasterOutputSize {
        val longest = max(width, height)
        if (longest <= MAX_OUTPUT_DIMENSION) return RasterOutputSize(width, height)
        val scale = longest.toDouble() / MAX_OUTPUT_DIMENSION.toDouble()
        return RasterOutputSize(
            width = ceil(width / scale).toInt(),
            height = ceil(height / scale).toInt(),
        )
    }

    private data class RasterStats(
        val validPixelCount: Int,
    )

    private data class RasterOutputSize(
        val width: Int,
        val height: Int,
    )

    companion object {
        private const val MAX_OUTPUT_DIMENSION = 2048
        private const val MAX_TIFF_BYTES = 128 * 1024 * 1024
    }
}
