package com.xywy.signalmap.domain.model

data class LayerPointIconStyle(
    val fillColor: String,
    val strokeColor: String,
    val targetMode: Boolean = false,
)

object LayerPointIconCatalog {
    fun style(preset: LayerPointIconPreset): LayerPointIconStyle = when (preset) {
        LayerPointIconPreset.RED_PIN -> LayerPointIconStyle(fillColor = "#EA4335", strokeColor = "#8B1E16")
        LayerPointIconPreset.BLUE_PIN -> LayerPointIconStyle(fillColor = "#4285F4", strokeColor = "#174EA6")
        LayerPointIconPreset.GREEN_PIN -> LayerPointIconStyle(fillColor = "#34A853", strokeColor = "#137333")
        LayerPointIconPreset.AMBER_PIN -> LayerPointIconStyle(fillColor = "#FBBC04", strokeColor = "#B06000")
        LayerPointIconPreset.PURPLE_PIN -> LayerPointIconStyle(fillColor = "#A142F4", strokeColor = "#5B21B6")
        LayerPointIconPreset.TARGET_DOT -> LayerPointIconStyle(fillColor = "#202124", strokeColor = "#FFFFFF", targetMode = true)
    }
}
