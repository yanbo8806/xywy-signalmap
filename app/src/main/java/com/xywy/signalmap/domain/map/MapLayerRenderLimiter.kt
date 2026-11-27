package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.SectorRecord

object MapLayerRenderLimiter {
    fun selectForSymbolLayer(
        records: List<SectorRecord>,
        limit: Int,
        alwaysIncludeLayerIds: Set<String>,
        alwaysIncludeLimit: Int = Int.MAX_VALUE,
    ): List<SectorRecord> {
        if (records.isEmpty()) return emptyList()
        if (alwaysIncludeLayerIds.isEmpty()) return selectFairly(records, limit)

        val forced = selectFairly(records.filter { it.layerId in alwaysIncludeLayerIds }, alwaysIncludeLimit)
        val limited = selectFairly(records.filterNot { it.layerId in alwaysIncludeLayerIds }, limit)
        return (forced + limited).distinctBy { it.id }
    }

    private fun selectFairly(records: List<SectorRecord>, limit: Int): List<SectorRecord> {
        if (limit <= 0) return emptyList()
        if (records.size <= limit) return records
        val quotas = layerQuotas(records.groupingBy { it.layerId }.eachCount(), limit).toMutableMap()
        // Preserve viewport/distance ordering within each layer, including at the smaller label cap.
        return records.filter { record ->
            val remaining = quotas.getValue(record.layerId)
            if (remaining <= 0) false else {
                quotas[record.layerId] = remaining - 1
                true
            }
        }
    }

    internal fun layerQuotas(counts: Map<String, Int>, limit: Int): Map<String, Int> {
        var remaining = limit.coerceAtLeast(0)
        val layers = counts.entries.sortedBy { it.value }
        return buildMap {
            layers.forEachIndexed { index, (layerId, count) ->
                val layersLeft = layers.size - index
                val share = remaining / layersLeft + if (remaining % layersLeft > 0) 1 else 0
                val quota = minOf(count, share)
                put(layerId, quota)
                remaining -= quota
            }
        }
    }
}
