package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.LayerDocument
import com.xywy.signalmap.domain.model.RadioTechnology

/** Compute once per catalog update, never once per signal/GPS update. */
data class EngineeringCatalogSummary(
    val bands: List<String> = emptyList(),
    val technologiesByLayer: Map<String, Set<RadioTechnology>> = emptyMap(),
) {
    companion object {
        fun create(documents: List<LayerDocument>): EngineeringCatalogSummary {
            val bands = sortedSetOf<String>()
            val technologies = linkedMapOf<String, Set<RadioTechnology>>()
            documents.forEach { document ->
                val layerTechnologies = mutableSetOf<RadioTechnology>()
                document.sectors.forEach { sector ->
                    sector.band?.takeIf(String::isNotBlank)?.let(bands::add)
                    layerTechnologies += sector.radioTechnology
                }
                technologies[document.meta.id] = layerTechnologies
            }
            return EngineeringCatalogSummary(bands.toList(), technologies)
        }
    }
}

/** Layer styles and visibility metadata may change while the loaded records stay identical. */
fun sameSectorLists(previous: List<List<com.xywy.signalmap.domain.model.SectorRecord>>,
                    next: List<List<com.xywy.signalmap.domain.model.SectorRecord>>): Boolean =
    previous.size == next.size && previous.indices.all { previous[it] === next[it] }
