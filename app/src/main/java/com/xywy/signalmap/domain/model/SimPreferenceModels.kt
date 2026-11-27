package com.xywy.signalmap.domain.model

enum class EngineeringLayerBindingMode {
    AUTO,
    DISABLED,
    SPECIFIC_LAYER,
}

data class EngineeringLayerBinding(
    val mode: EngineeringLayerBindingMode = EngineeringLayerBindingMode.AUTO,
    val layerId: String? = null,
) {
    companion object {
        val Auto = EngineeringLayerBinding()
        val Disabled = EngineeringLayerBinding(mode = EngineeringLayerBindingMode.DISABLED)

        fun specificLayer(layerId: String): EngineeringLayerBinding =
            EngineeringLayerBinding(
                mode = EngineeringLayerBindingMode.SPECIFIC_LAYER,
                layerId = layerId,
            )
    }
}

data class SimTechEngineeringBinding(
    val nr: EngineeringLayerBinding = EngineeringLayerBinding.Auto,
    val lte: EngineeringLayerBinding = EngineeringLayerBinding.Auto,
) {
    fun forTechnology(technology: RadioTechnology): EngineeringLayerBinding = when (technology) {
        RadioTechnology.NR -> nr
        RadioTechnology.LTE -> lte
        else -> EngineeringLayerBinding.Auto
    }
}

data class SimEngineeringBindings(
    val perSlot: Map<Int, SimTechEngineeringBinding> = emptyMap(),
) {
    fun forSlot(slotIndex: Int): SimTechEngineeringBinding =
        perSlot[slotIndex] ?: SimTechEngineeringBinding()

    fun forSignal(slotIndex: Int, technology: RadioTechnology): EngineeringLayerBinding =
        forSlot(slotIndex).forTechnology(technology)
}
