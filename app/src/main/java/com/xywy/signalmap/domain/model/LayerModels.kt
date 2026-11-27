package com.xywy.signalmap.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ImportedLayerRenderMode {
    AUTO,
    POINT,
    SECTOR,
    USER,
}

@Serializable
data class RasterLayerInfo(
    val width: Int,
    val height: Int,
    val validPixelCount: Int,
    val colorHex: String = DEFAULT_RASTER_COLOR_HEX,
    val opacity: Float = DEFAULT_RASTER_OPACITY,
    val topLeft: GeoPoint,
    val topRight: GeoPoint,
    val bottomRight: GeoPoint,
    val bottomLeft: GeoPoint,
) {
    val minLongitude: Double
        get() = minOf(topLeft.longitude, topRight.longitude, bottomRight.longitude, bottomLeft.longitude)
    val maxLongitude: Double
        get() = maxOf(topLeft.longitude, topRight.longitude, bottomRight.longitude, bottomLeft.longitude)
    val minLatitude: Double
        get() = minOf(topLeft.latitude, topRight.latitude, bottomRight.latitude, bottomLeft.latitude)
    val maxLatitude: Double
        get() = maxOf(topLeft.latitude, topRight.latitude, bottomRight.latitude, bottomLeft.latitude)

    companion object {
        const val DEFAULT_RASTER_COLOR_HEX = "#FF4D4F"
        const val DEFAULT_RASTER_OPACITY = 0.7f
    }
}

@Serializable
enum class LayerPointIconPreset {
    RED_PIN,
    BLUE_PIN,
    GREEN_PIN,
    AMBER_PIN,
    PURPLE_PIN,
    TARGET_DOT;

    val iconId: String
        get() = when (this) {
            RED_PIN -> "point-icon-red-pin"
            BLUE_PIN -> "point-icon-blue-pin"
            GREEN_PIN -> "point-icon-green-pin"
            AMBER_PIN -> "point-icon-amber-pin"
            PURPLE_PIN -> "point-icon-purple-pin"
            TARGET_DOT -> "point-icon-target-dot"
        }

    val title: String
        get() = when (this) {
            RED_PIN -> "红色图钉"
            BLUE_PIN -> "蓝色图钉"
            GREEN_PIN -> "绿色图钉"
            AMBER_PIN -> "橙色图钉"
            PURPLE_PIN -> "紫色图钉"
            TARGET_DOT -> "目标圆点"
        }
}

@Serializable
data class LayerMetadata(
    val id: String,
    val name: String,
    val sourceFileName: String,
    val importedAtMillis: Long,
    val isVisible: Boolean,
    val recordCount: Int,
    val sheetName: String? = null,
    val renderMode: ImportedLayerRenderMode = ImportedLayerRenderMode.AUTO,
    val detailFieldHeaders: List<String> = emptyList(),
    val showLabels: Boolean = true,
    val labelTextColor: String? = null,
    val labelHaloColor: String? = null,
    val pointIconPreset: LayerPointIconPreset = LayerPointIconPreset.RED_PIN,
    val userEditable: Boolean = false,
    // Off by default: use the shared nearest-record limit for this layer.
    val alwaysRenderAllRecords: Boolean = false,
    val rasterInfo: RasterLayerInfo? = null,
)

val LayerMetadata.isRaster: Boolean
    get() = rasterInfo != null

@Serializable
data class LayerDocument(
    val meta: LayerMetadata,
    val sectors: List<SectorRecord>,
)
