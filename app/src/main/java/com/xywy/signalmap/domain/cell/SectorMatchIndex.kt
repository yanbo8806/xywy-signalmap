package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.EngineeringLayerBindingMode
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.SimEngineeringBindings

class SectorMatchIndex private constructor(
    private val autoScopes: Map<RadioTechnology, SectorLookupScope>,
    private val specificScopes: Map<ScopeKey, SectorLookupScope>,
) {
    fun scope(
        slotIndex: Int,
        technology: RadioTechnology,
        layerBindings: SimEngineeringBindings,
    ): SectorLookupScope {
        val binding = layerBindings.forSignal(slotIndex, technology)
        return when (binding.mode) {
            EngineeringLayerBindingMode.AUTO -> autoScopes[technology] ?: SectorLookupScope.Empty
            EngineeringLayerBindingMode.DISABLED -> SectorLookupScope.Empty
            EngineeringLayerBindingMode.SPECIFIC_LAYER -> {
                val layerId = binding.layerId ?: return SectorLookupScope.Empty
                specificScopes[ScopeKey(layerId = layerId, technology = technology)] ?: SectorLookupScope.Empty
            }
        }
    }

    companion object {
        val Empty: SectorMatchIndex = SectorMatchIndex(
            autoScopes = emptyMap(),
            specificScopes = emptyMap(),
        )

        fun build(sectors: List<SectorRecord>): SectorMatchIndex {
            if (sectors.isEmpty()) return Empty
            val autoBuilders = linkedMapOf<RadioTechnology, SectorLookupScope.Builder>()
            val specificBuilders = linkedMapOf<ScopeKey, SectorLookupScope.Builder>()
            val layersByTechnology = linkedMapOf<RadioTechnology, MutableSet<String>>()
            sectors.forEach { layersByTechnology.getOrPut(it.radioTechnology) { linkedSetOf() }.add(it.layerId) }
            sectors.forEach { sector ->
                val aliases = sector.cellIdentityAliases()
                if (layersByTechnology.getValue(sector.radioTechnology).size > 1) {
                    autoBuilders.getOrPut(sector.radioTechnology) { SectorLookupScope.Builder() }.add(sector, aliases)
                }
                specificBuilders.getOrPut(ScopeKey(layerId = sector.layerId, technology = sector.radioTechnology)) {
                    SectorLookupScope.Builder()
                }.add(sector, aliases)
            }
            val specificScopes = specificBuilders.mapValues { (_, builder) -> builder.build() }
            return SectorMatchIndex(
                // A technology in one layer has exactly the same AUTO and explicit scope.
                autoScopes = layersByTechnology.mapValues { (technology, layers) ->
                    autoBuilders[technology]?.build() ?: specificScopes.getValue(ScopeKey(layers.single(), technology))
                },
                specificScopes = specificScopes,
            )
        }
    }

    private data class ScopeKey(
        val layerId: String,
        val technology: RadioTechnology,
    )
}

class SectorLookupScope private constructor(
    val all: List<SectorRecord>,
    private val byCellIdIndex: Map<String, List<SectorRecord>>,
    private val byPciIndex: Map<String, List<SectorRecord>>,
) {
    fun byCellId(cellId: String?): List<SectorRecord> = cellId
        .normalizedOrNull()
        ?.let { byCellIdIndex[it] }
        .orEmpty()

    fun byPci(pci: String?): List<SectorRecord> = pci
        .normalizedOrNull()
        ?.let { byPciIndex[it] }
        .orEmpty()

    class Builder(initialCapacity: Int = 16) {
        private val all = ArrayList<SectorRecord>(initialCapacity)
        private val byCellIdIndex = linkedMapOf<String, MutableList<SectorRecord>>()
        private val byPciIndex = linkedMapOf<String, MutableList<SectorRecord>>()
        private val seenCellAliases = hashSetOf<String>()

        fun add(record: SectorRecord) = add(record, record.cellIdentityAliases())

        internal fun add(record: SectorRecord, aliases: Set<String>) {
            all += record
            aliases.forEach { alias ->
                val key = "${alias}|${record.id}"
                if (seenCellAliases.add(key)) {
                    byCellIdIndex.getOrPut(alias) { mutableListOf() } += record
                }
            }
            record.pci.normalizedOrNull()?.let { pci ->
                byPciIndex.getOrPut(pci) { mutableListOf() } += record
            }
        }

        fun build(): SectorLookupScope {
            if (all.isEmpty()) return Empty
            return SectorLookupScope(
                all = all.toList(),
                byCellIdIndex = byCellIdIndex.mapValues { (_, records) -> records.toList() },
                byPciIndex = byPciIndex.mapValues { (_, records) -> records.toList() },
            )
        }
    }

    companion object {
        val Empty: SectorLookupScope = SectorLookupScope(
            all = emptyList(),
            byCellIdIndex = emptyMap(),
            byPciIndex = emptyMap(),
        )

        fun build(records: List<SectorRecord>): SectorLookupScope {
            if (records.isEmpty()) return Empty
            return Builder(records.size).apply { records.forEach(::add) }.build()
        }
    }
}

private fun String?.normalizedOrNull(): String? = this
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.uppercase()
