package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.SectorRecord
import java.util.PriorityQueue
import kotlin.math.cos

data class GeoBounds(
    val minLongitude: Double,
    val maxLongitude: Double,
    val minLatitude: Double,
    val maxLatitude: Double,
) {
    fun contains(
        longitude: Double,
        latitude: Double,
    ): Boolean = longitude in minLongitude..maxLongitude && latitude in minLatitude..maxLatitude
}

object MapSectorRenderPlanner {
    fun selectSectors(
        records: List<SectorRecord>,
        focusLongitude: Double,
        focusLatitude: Double,
        bounds: GeoBounds?,
        limit: Int = 400,
        spatialIndex: SectorSpatialIndex? = null,
        alwaysIncludeLayerIds: Set<String> = emptySet(),
        alwaysIncludeLimit: Int = Int.MAX_VALUE,
    ): List<SectorRecord> {
        if (records.isEmpty()) return emptyList()
        // The background index can briefly lag behind an import or visibility change.
        val index = spatialIndex?.takeIf { !it.isEmpty && it.isFor(records) }
        val forcedRecords = if (alwaysIncludeLayerIds.isEmpty() || alwaysIncludeLimit <= 0) {
            emptyList()
        } else {
            viewportFirstNearest(
                records = records,
                focusLongitude = focusLongitude,
                focusLatitude = focusLatitude,
                bounds = bounds,
                limit = alwaysIncludeLimit,
                spatialIndex = index,
                includeLayerIds = alwaysIncludeLayerIds,
            )
        }
        if (limit <= 0) return forcedRecords

        val forcedIds = forcedRecords.mapTo(hashSetOf()) { it.id }
        val normalRecords = if (alwaysIncludeLayerIds.isEmpty()) {
            records.filterNot { it.id in forcedIds }
        } else {
            records.filterNot { it.id in forcedIds || it.layerId in alwaysIncludeLayerIds }
        }
        val limitedRecords = viewportFirstNearest(
            records = normalRecords,
            focusLongitude = focusLongitude,
            focusLatitude = focusLatitude,
            bounds = bounds,
            limit = limit,
            spatialIndex = index,
            excludeIds = forcedIds,
        )
        if (forcedRecords.isEmpty()) return limitedRecords
        return (forcedRecords + limitedRecords).distinctBy { it.id }
    }

    private fun viewportFirstNearest(
        records: List<SectorRecord>,
        focusLongitude: Double,
        focusLatitude: Double,
        bounds: GeoBounds?,
        limit: Int,
        spatialIndex: SectorSpatialIndex?,
        excludeIds: Set<String> = emptySet(),
        includeLayerIds: Set<String> = emptySet(),
    ): List<SectorRecord> {
        if (records.isEmpty() || limit <= 0) return emptyList()
        val layerFilter = includeLayerIds.takeIf { it.isNotEmpty() }
        val eligible = records.filter {
            it.id !in excludeIds && (layerFilter == null || it.layerId in layerFilter)
        }
        val byLayer = eligible.groupBy { it.layerId }
        val quotas = MapLayerRenderLimiter.layerQuotas(byLayer.mapValues { it.value.size }, limit)
        val viewportByLayer = if (bounds == null) emptyMap() else {
            (spatialIndex?.queryBounds(bounds, includeLayerIds = byLayer.keys) ?: eligible)
                .filter { it.id !in excludeIds && bounds.contains(it.longitude, it.latitude) }
                .groupBy { it.layerId }
        }
        val selected = byLayer.flatMap { (layerId, layerRecords) ->
            val quota = quotas.getValue(layerId)
            if (quota <= 0) return@flatMap emptyList()
            val inViewport = nearest(
                records = viewportByLayer[layerId].orEmpty(),
                focusLongitude = focusLongitude,
                focusLatitude = focusLatitude,
                limit = quota,
            )
            val remaining = quota - inViewport.size
            if (remaining <= 0) return@flatMap inViewport
            val selectedIds = inViewport.mapTo(hashSetOf()) { it.id }
            val remainder = if (byLayer.size == 1 && spatialIndex != null) {
                spatialIndex.queryNearest(
                    focusLongitude = focusLongitude,
                    focusLatitude = focusLatitude,
                    limit = remaining,
                    excludeIds = excludeIds + selectedIds,
                    includeLayerIds = setOf(layerId),
                )
            } else {
                nearest(
                    records = layerRecords.filterNot { it.id in selectedIds },
                    focusLongitude = focusLongitude,
                    focusLatitude = focusLatitude,
                    limit = remaining,
                )
            }
            inViewport + remainder
        }
        return selected.sortedWith(
            compareBy<SectorRecord> { bounds != null && !bounds.contains(it.longitude, it.latitude) }
                .thenBy { distanceScore(it.longitude, it.latitude, focusLongitude, focusLatitude) },
        )
    }

    private fun nearest(
        records: List<SectorRecord>,
        focusLongitude: Double,
        focusLatitude: Double,
        limit: Int,
    ): List<SectorRecord> {
        if (records.size <= limit) {
            return records.sortedBy { distanceScore(it.longitude, it.latitude, focusLongitude, focusLatitude) }
        }

        val heap = PriorityQueue<ScoredSector>(compareByDescending { it.score })
        records.forEach { record ->
            val score = distanceScore(record.longitude, record.latitude, focusLongitude, focusLatitude)
            if (heap.size < limit) {
                heap += ScoredSector(score, record)
            } else {
                val currentWorst = heap.peek()?.score ?: return@forEach
                if (score < currentWorst) {
                    heap.poll()
                    heap += ScoredSector(score, record)
                }
            }
        }
        return heap.toList()
            .sortedBy { it.score }
            .map { it.record }
    }

    private fun distanceScore(
        longitude: Double,
        latitude: Double,
        focusLongitude: Double,
        focusLatitude: Double,
    ): Double {
        val latitudeFactor = cos(Math.toRadians((latitude + focusLatitude) / 2.0))
        val dx = (longitude - focusLongitude) * latitudeFactor
        val dy = latitude - focusLatitude
        return dx * dx + dy * dy
    }

    private data class ScoredSector(
        val score: Double,
        val record: SectorRecord,
    )
}
