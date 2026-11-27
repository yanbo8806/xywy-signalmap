package com.xywy.signalmap.domain.model

import com.xywy.signalmap.EngineeringLayerOptionUiState

object EngineeringLayerBindingOptionPolicy {
    fun optionsFor(
        options: List<EngineeringLayerOptionUiState>,
        technology: RadioTechnology,
    ): List<EngineeringLayerOptionUiState> {
        val preferred = ArrayList<EngineeringLayerOptionUiState>(options.size)
        val fallback = ArrayList<EngineeringLayerOptionUiState>(options.size)
        options.forEach { option ->
            if (option.supports(technology)) {
                preferred += option
            } else {
                fallback += option
            }
        }
        return preferred + fallback
    }
}

private fun EngineeringLayerOptionUiState.supports(technology: RadioTechnology): Boolean = when (technology) {
    RadioTechnology.NR -> supportsNr
    RadioTechnology.LTE -> supportsLte
    else -> false
}
