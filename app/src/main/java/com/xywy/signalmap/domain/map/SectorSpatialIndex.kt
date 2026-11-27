package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.SectorRecord
import java.util.PriorityQueue
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

class SectorSpatialIndex private constructor(
    private val sourceRecords: List<SectorRecord>,
    private val cellSizeMeters: Double,
    private val buckets: Map<GridCell, List<IndexedSector>>,
    private val allIndexedSectors: List<IndexedSector>,
    private val minCellX: Int,
    private val maxCellX: Int,
    private val minCellY: Int,
    private val maxCellY: Int,
) {
    val isEmpty: Boolean
        get() = buckets.isEmpty()

    fun isFor(records: List<SectorRecord>): Boolean = sourceRecords === records

    fun queryBounds(
        bounds: GeoBounds,
        includeLayerIds: Set<String>? = null,
    ): List<SectorRecord>? {
        if (isEmpty) return emptyList()
        if (includeLayerIds != null && includeLayerIds.isEmpty()) return emptyList()
        val minProjected = project(bounds.minLongitude, bounds.minLatitude)
        val maxProjected = project(bounds.maxLongitude, bounds.maxLatitude)
        val startX = maxOf(floor(minOf(minProjected.x, maxProjected.x) / cellSizeMeters).toInt(), minCellX)
        val endX = minOf(floor(maxOf(minProjected.x, maxProjected.x) / cellSizeMeters).toInt(), maxCellX)
        val startY = maxOf(floor(minOf(minProjected.y, maxProjected.y) / cellSizeMeters).toInt(), minCellY)
        val endY = minOf(floor(maxOf(minProjected.y, maxProjected.y) / cellSizeMeters).toInt(), maxCellY)
        if (startX > endX || startY > endY) return emptyList()
        val width = endX - startX + 1L
        val height = endY - startY + 1L
        if (width * height > DEFAULT_MAX_QUERY_CELLS) return null
        val results = mutableListOf<SectorRecord>()
        for (cellX in startX..endX) {
            for (cellY in startY..endY) {
                buckets[GridCell(cellX, cellY)].orEmpty().forEach { indexed ->
                    if (includeLayerIds != null && indexed.record.layerId !in includeLayerIds) return@forEach
                    if (bounds.contains(indexed.record.longitude, indexed.record.latitude)) {
                        results += indexed.record
                    }
                }
            }
        }
        return results
    }

    fun queryNearest(
        focusLongitude: Double,
        focusLatitude: Double,
        limit: Int,
        excludeIds: Set<String> = emptySet(),
        includeIds: Set<String>? = null,
        includeLayerIds: Set<String>? = null,
    ): List<SectorRecord> {
        if (isEmpty || limit <= 0) return emptyList()
        if (includeIds != null && includeIds.isEmpty()) return emptyList()
        if (includeLayerIds != null && includeLayerIds.isEmpty()) return emptyList()
        val focus = project(focusLongitude, focusLatitude)
        val originCell = toCell(focus)
        if (minimumRadiusToReachBounds(originCell) > MAX_EMPTY_RING_RADIUS_BEFORE_LINEAR) {
            return linearNearest(
                focus = focus,
                limit = limit,
                excludeIds = excludeIds,
                includeIds = includeIds,
                includeLayerIds = includeLayerIds,
            )
        }

        val candidates = PriorityQueue<ScoredRecord>(compareByDescending { it.score })
        var visitedCellCount = 0
        var radius = 0

        while (true) {
            ringCells(originCell, radius).forEach { cell ->
                visitedCellCount += 1
                if (visitedCellCount > MAX_RING_SCAN_CELLS) {
                    return linearNearest(
                        focus = focus,
                        limit = limit,
                        excludeIds = excludeIds,
                        includeIds = includeIds,
                        includeLayerIds = includeLayerIds,
                    )
                }
                buckets[cell].orEmpty().forEach { indexed ->
                    if (indexed.record.id in excludeIds) return@forEach
                    if (includeIds != null && indexed.record.id !in includeIds) return@forEach
                    if (includeLayerIds != null && indexed.record.layerId !in includeLayerIds) return@forEach
                    val score = squaredDistance(indexed.projectedX, indexed.projectedY, focus.x, focus.y)
                    offerCandidate(
                        heap = candidates,
                        limit = limit,
                        candidate = ScoredRecord(score = score, record = indexed.record),
                    )
                }
            }
            if (candidates.size >= limit) {
                val worstScore = candidates.peek()?.score ?: Double.MAX_VALUE
                val outsideLowerBound = radius * cellSizeMeters
                if (radius > 0 && worstScore <= outsideLowerBound * outsideLowerBound) {
                    return candidates.toSortedRecords()
                }
            }
            if (radius >= maxRingRadius(originCell)) break
            radius += 1
        }

        return candidates.toSortedRecords()
    }

    private fun maxRingRadius(originCell: GridCell): Int {
        val maxDx = maxOf(originCell.x - minCellX, maxCellX - originCell.x)
        val maxDy = maxOf(originCell.y - minCellY, maxCellY - originCell.y)
        return maxOf(maxDx, maxDy)
    }

    private fun minimumRadiusToReachBounds(originCell: GridCell): Int {
        val dx = when {
            originCell.x < minCellX -> minCellX - originCell.x
            originCell.x > maxCellX -> originCell.x - maxCellX
            else -> 0
        }
        val dy = when {
            originCell.y < minCellY -> minCellY - originCell.y
            originCell.y > maxCellY -> originCell.y - maxCellY
            else -> 0
        }
        return maxOf(dx, dy)
    }

    private fun toCell(projected: ProjectedPoint): GridCell {
        return GridCell(
            x = floor(projected.x / cellSizeMeters).toInt(),
            y = floor(projected.y / cellSizeMeters).toInt(),
        )
    }

    private fun ringCells(
        origin: GridCell,
        radius: Int,
    ): Sequence<GridCell> = sequence {
        if (radius == 0) {
            if (origin.x in minCellX..maxCellX && origin.y in minCellY..maxCellY) {
                yield(origin)
            }
            return@sequence
        }

        suspend fun SequenceScope<GridCell>.emitHorizontal(y: Int) {
            if (y !in minCellY..maxCellY) return
            val startX = maxOf(minCellX, origin.x - radius)
            val endX = minOf(maxCellX, origin.x + radius)
            if (startX > endX) return
            for (x in startX..endX) {
                yield(GridCell(x, y))
            }
        }

        suspend fun SequenceScope<GridCell>.emitVertical(x: Int) {
            if (x !in minCellX..maxCellX) return
            val startY = maxOf(minCellY, origin.y - radius + 1)
            val endY = minOf(maxCellY, origin.y + radius - 1)
            if (startY > endY) return
            for (y in startY..endY) {
                yield(GridCell(x, y))
            }
        }

        emitHorizontal(origin.y - radius)
        emitHorizontal(origin.y + radius)
        emitVertical(origin.x - radius)
        emitVertical(origin.x + radius)
    }

    private fun linearNearest(
        focus: ProjectedPoint,
        limit: Int,
        excludeIds: Set<String>,
        includeIds: Set<String>? = null,
        includeLayerIds: Set<String>? = null,
    ): List<SectorRecord> {
        val heap = PriorityQueue<ScoredRecord>(compareByDescending { it.score })
        allIndexedSectors.forEach { indexed ->
            if (indexed.record.id in excludeIds) return@forEach
            if (includeIds != null && indexed.record.id !in includeIds) return@forEach
            if (includeLayerIds != null && indexed.record.layerId !in includeLayerIds) return@forEach
            val score = squaredDistance(indexed.projectedX, indexed.projectedY, focus.x, focus.y)
            offerCandidate(
                heap = heap,
                limit = limit,
                candidate = ScoredRecord(score = score, record = indexed.record),
            )
        }
        return heap.toSortedRecords()
    }

    companion object {
        val Empty: SectorSpatialIndex = SectorSpatialIndex(
            sourceRecords = emptyList(),
            cellSizeMeters = 40.0,
            buckets = emptyMap(),
            allIndexedSectors = emptyList(),
            minCellX = 0,
            maxCellX = -1,
            minCellY = 0,
            maxCellY = -1,
        )

        fun build(
            records: List<SectorRecord>,
            cellSizeMeters: Double = 40.0,
        ): SectorSpatialIndex {
            if (records.isEmpty()) return Empty
            val buckets = linkedMapOf<GridCell, MutableList<IndexedSector>>()
            val indexedSectors = ArrayList<IndexedSector>(records.size)
            var minCellX = Int.MAX_VALUE
            var maxCellX = Int.MIN_VALUE
            var minCellY = Int.MAX_VALUE
            var maxCellY = Int.MIN_VALUE
            records.forEach { record ->
                val projected = project(record.longitude, record.latitude)
                val cell = GridCell(
                    x = floor(projected.x / cellSizeMeters).toInt(),
                    y = floor(projected.y / cellSizeMeters).toInt(),
                )
                val indexedSector = IndexedSector(
                    record = record,
                    projectedX = projected.x,
                    projectedY = projected.y,
                )
                buckets.getOrPut(cell) { mutableListOf() } += indexedSector
                indexedSectors += indexedSector
                minCellX = minOf(minCellX, cell.x)
                maxCellX = maxOf(maxCellX, cell.x)
                minCellY = minOf(minCellY, cell.y)
                maxCellY = maxOf(maxCellY, cell.y)
            }
            return SectorSpatialIndex(
                sourceRecords = records,
                cellSizeMeters = cellSizeMeters,
                buckets = buckets.mapValues { it.value.toList() },
                allIndexedSectors = indexedSectors,
                minCellX = minCellX,
                maxCellX = maxCellX,
                minCellY = minCellY,
                maxCellY = maxCellY,
            )
        }

        private fun offerCandidate(
            heap: PriorityQueue<ScoredRecord>,
            limit: Int,
            candidate: ScoredRecord,
        ) {
            if (heap.size < limit) {
                heap += candidate
                return
            }
            val currentWorst = heap.peek()?.score ?: Double.MAX_VALUE
            if (candidate.score < currentWorst) {
                heap.poll()
                heap += candidate
            }
        }

        private fun project(
            longitude: Double,
            latitude: Double,
        ): ProjectedPoint {
            val clampedLatitude = latitude.coerceIn(-85.05112878, 85.05112878)
            val x = EARTH_RADIUS_METERS * Math.toRadians(longitude)
            val y = EARTH_RADIUS_METERS * ln(tan(PI / 4.0 + Math.toRadians(clampedLatitude) / 2.0))
            return ProjectedPoint(x = x, y = y)
        }

        private fun squaredDistance(
            x1: Double,
            y1: Double,
            x2: Double,
            y2: Double,
        ): Double = (x1 - x2).pow(2) + (y1 - y2).pow(2)

        private const val EARTH_RADIUS_METERS = 6_378_137.0
        private const val DEFAULT_MAX_QUERY_CELLS = 4_096L
        private const val MAX_EMPTY_RING_RADIUS_BEFORE_LINEAR = 256
        private const val MAX_RING_SCAN_CELLS = 4_096
    }

    private data class GridCell(
        val x: Int,
        val y: Int,
    )

    private data class ProjectedPoint(
        val x: Double,
        val y: Double,
    )

    private data class IndexedSector(
        val record: SectorRecord,
        val projectedX: Double,
        val projectedY: Double,
    )

    private data class ScoredRecord(
        val score: Double,
        val record: SectorRecord,
    )

    private fun PriorityQueue<ScoredRecord>.toSortedRecords(): List<SectorRecord> = toList()
        .sortedBy { it.score }
        .map { it.record }
}
