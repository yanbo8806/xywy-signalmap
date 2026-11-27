package com.xywy.signalmap.data.local

import android.graphics.Bitmap
import com.xywy.signalmap.domain.model.RasterLayerColorizer
import java.io.File

internal object RasterLayerAssetPaths {
    private const val RASTER_DIR_NAME = "raster-assets"
    private const val MASK_FILE_NAME = "mask.bin"
    private const val RENDER_FILE_NAME = "render.png"
    private const val VERSION_FILE_NAME = "render.version"

    const val CURRENT_RENDER_VERSION = 2

    fun rasterDir(rootDir: File, layerId: String): File =
        File(File(rootDir, RASTER_DIR_NAME), layerId)

    fun maskFile(rootDir: File, layerId: String): File =
        File(rasterDir(rootDir, layerId), MASK_FILE_NAME)

    fun renderFile(rootDir: File, layerId: String): File =
        File(rasterDir(rootDir, layerId), RENDER_FILE_NAME)

    fun versionFile(rootDir: File, layerId: String): File =
        File(rasterDir(rootDir, layerId), VERSION_FILE_NAME)
}

internal class RasterLayerAssetStore(
    private val rootDir: File,
) {
    fun write(
        layerId: String,
        width: Int,
        height: Int,
        alphaMask: ByteArray,
        colorHex: String,
        opacity: Float,
    ) {
        require(alphaMask.size == width * height) {
            "栅格掩膜大小不匹配：expected=${width * height} actual=${alphaMask.size}"
        }
        val dir = RasterLayerAssetPaths.rasterDir(rootDir, layerId).apply { mkdirs() }
        RasterLayerAssetPaths.maskFile(rootDir, layerId).writeBytes(alphaMask)
        writeRenderedPng(
            file = RasterLayerAssetPaths.renderFile(rootDir, layerId),
            width = width,
            height = height,
            alphaMask = alphaMask,
            colorHex = colorHex,
            opacity = opacity,
        )
    }

    fun recolor(
        layerId: String,
        width: Int,
        height: Int,
        colorHex: String,
        opacity: Float,
    ) {
        val maskFile = RasterLayerAssetPaths.maskFile(rootDir, layerId)
        require(maskFile.exists()) { "未找到栅格掩膜文件：${maskFile.absolutePath}" }
        val alphaMask = maskFile.readBytes()
        require(alphaMask.size == width * height) {
            "栅格掩膜大小不匹配：expected=${width * height} actual=${alphaMask.size}"
        }
        writeRenderedPng(
            file = RasterLayerAssetPaths.renderFile(rootDir, layerId),
            width = width,
            height = height,
            alphaMask = alphaMask,
            colorHex = colorHex,
            opacity = opacity,
        )
    }

    fun delete(layerId: String) {
        RasterLayerAssetPaths.rasterDir(rootDir, layerId).deleteRecursively()
    }

    fun needsRefresh(layerId: String): Boolean {
        val renderFile = RasterLayerAssetPaths.renderFile(rootDir, layerId)
        if (!renderFile.exists()) return true
        val versionValue = RasterLayerAssetPaths.versionFile(rootDir, layerId)
            .takeIf(File::exists)
            ?.readText()
            ?.trim()
            ?.toIntOrNull()
        return versionValue != RasterLayerAssetPaths.CURRENT_RENDER_VERSION
    }

    private fun writeRenderedPng(
        file: File,
        width: Int,
        height: Int,
        alphaMask: ByteArray,
        colorHex: String,
        opacity: Float,
    ) {
        file.parentFile?.mkdirs()
        val pixels = RasterLayerColorizer.colorize(
            alphaMask = alphaMask,
            colorHex = colorHex,
            opacity = opacity,
        )
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            RasterLayerAssetPaths.versionFile(rootDir, file.parentFile?.name.orEmpty())
                .writeText(RasterLayerAssetPaths.CURRENT_RENDER_VERSION.toString())
        } finally {
            bitmap.recycle()
        }
    }
}
