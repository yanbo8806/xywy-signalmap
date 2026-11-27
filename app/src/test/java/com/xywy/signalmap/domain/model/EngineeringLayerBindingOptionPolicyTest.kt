package com.xywy.signalmap.domain.model

import com.xywy.signalmap.EngineeringLayerOptionUiState
import kotlin.test.Test
import kotlin.test.assertEquals

class EngineeringLayerBindingOptionPolicyTest {
    @Test
    fun `nr binding keeps all imported layers selectable and prioritizes nr capable layers`() {
        val options = listOf(
            EngineeringLayerOptionUiState(
                layerId = "lte-only",
                name = "LTE工参",
                supportsNr = false,
                supportsLte = true,
            ),
            EngineeringLayerOptionUiState(
                layerId = "nr-and-lte",
                name = "双模工参",
                supportsNr = true,
                supportsLte = true,
            ),
            EngineeringLayerOptionUiState(
                layerId = "nr-only",
                name = "NR工参",
                supportsNr = true,
                supportsLte = false,
            ),
        )

        val result = EngineeringLayerBindingOptionPolicy.optionsFor(
            options = options,
            technology = RadioTechnology.NR,
        )

        assertEquals(
            listOf("nr-and-lte", "nr-only", "lte-only"),
            result.map { it.layerId },
        )
    }

    @Test
    fun `lte binding keeps all imported layers selectable and prioritizes lte capable layers`() {
        val options = listOf(
            EngineeringLayerOptionUiState(
                layerId = "nr-only",
                name = "NR工参",
                supportsNr = true,
                supportsLte = false,
            ),
            EngineeringLayerOptionUiState(
                layerId = "nr-and-lte",
                name = "双模工参",
                supportsNr = true,
                supportsLte = true,
            ),
            EngineeringLayerOptionUiState(
                layerId = "lte-only",
                name = "LTE工参",
                supportsNr = false,
                supportsLte = true,
            ),
        )

        val result = EngineeringLayerBindingOptionPolicy.optionsFor(
            options = options,
            technology = RadioTechnology.LTE,
        )

        assertEquals(
            listOf("nr-and-lte", "lte-only", "nr-only"),
            result.map { it.layerId },
        )
    }
}
