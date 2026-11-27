package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.SectorRecord
import java.util.PriorityQueue
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** A separate nearest-N budget for EACH layer, measured from the raw WGS84 map center. */
object NearbyLayerSelector {
    suspend fun select(
        records: List<SectorRecord>,
        layers: List<LayerMetadata>,
        center: GeoPoint,
        limit: Int,
    ): List<SectorRecord> {
        require(limit > 0)
        val context = currentCoroutineContext()
        val visibleLayers = layers.filter { it.isVisible }.associateBy { it.id }
        val queues = mutableMapOf<String, PriorityQueue<Candidate>>()
        val selected = ArrayList<Candidate>()
        val nearestFirst = compareBy<Candidate> { it.distance }.thenBy { it.index }
        val centerLatitude = Math.toRadians(center.latitude)
        records.forEachIndexed { index, record ->
            if (index % 128 == 0) context.ensureActive()
            val layer = visibleLayers[record.layerId] ?: return@forEachIndexed
            if (layer.alwaysRenderAllRecords) {
                selected.add(Candidate(record, index, 0.0))
            } else {
                val lat = Math.toRadians(record.latitude)
                val dLat = sin((lat - centerLatitude) / 2)
                val dLon = sin(Math.toRadians(record.longitude - center.longitude) / 2)
                // Haversine score is monotonic with distance, including across the date line.
                val score = dLat * dLat + cos(centerLatitude) * cos(lat) * dLon * dLon
                val queue = queues.getOrPut(layer.id) { PriorityQueue(nearestFirst.reversed()) }
                val candidate = Candidate(record, index, score)
                if (queue.size < limit) queue.add(candidate)
                else if (nearestFirst.compare(candidate, queue.peek()) < 0) {
                    queue.poll()
                    queue.add(candidate)
                }
            }
        }
        queues.values.forEach { selected.addAll(it) }
        context.ensureActive()
        // Stable source order also keeps co-located site deduplication deterministic.
        selected.sortBy { it.index }
        context.ensureActive()
        return selected.map { it.record }
    }

    private data class Candidate(val record: SectorRecord, val index: Int, val distance: Double)
}
