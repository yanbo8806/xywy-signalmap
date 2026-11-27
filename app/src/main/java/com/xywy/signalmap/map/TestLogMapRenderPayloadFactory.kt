package com.xywy.signalmap.map

import com.xywy.signalmap.domain.map.MapCoordinateProjector
import com.xywy.signalmap.domain.model.TestLogSample
import com.xywy.signalmap.domain.model.TestMetricMode
import com.xywy.signalmap.domain.model.TilePreset
import com.xywy.signalmap.domain.model.TilePresetId
import com.xywy.signalmap.domain.model.TilePresets
import org.maplibre.geojson.FeatureCollection

data class TestLogMapRenderPayload(
    val features: FeatureCollection,
    val signature: String,
) {
    // Factory runs off the UI thread; native source updates receive serialized data.
    val geoJson: String = features.toJson()

    companion object {
        val Empty = TestLogMapRenderPayload(
            features = FeatureCollection.fromFeatures(emptyList()),
            signature = "empty",
        )
    }
}

object TestLogMapRenderPayloadFactory {
    fun create(
        samples: List<TestLogSample>,
        metricMode: TestMetricMode,
        visibleSlotIndexes: Set<Int>,
        tilePreset: TilePreset,
        maxSamples: Int = DEFAULT_MAX_SAMPLES,
    ): TestLogMapRenderPayload {
        val renderSamples = samples.decimate(maxSamples)
        val projectedSamples = MapCoordinateProjector.toDisplayTestSamples(renderSamples, tilePreset)
        val features = GeoJsonFeatureFactory.testLogPointFeatures(
            samples = projectedSamples,
            metricMode = metricMode,
            visibleSlotIndexes = visibleSlotIndexes,
        )
        return TestLogMapRenderPayload(
            features = features,
            signature = buildSignature(
                samples = samples,
                renderSamples = renderSamples,
                metricMode = metricMode,
                visibleSlotIndexes = visibleSlotIndexes,
                tilePresetKey = tilePreset.key,
            ),
        )
    }

    fun create(
        samples: List<TestLogSample>,
        metricMode: TestMetricMode,
        visibleSlotIndexes: Set<Int>,
        tilePresetId: TilePresetId,
        maxSamples: Int = DEFAULT_MAX_SAMPLES,
    ): TestLogMapRenderPayload = create(
        samples = samples,
        metricMode = metricMode,
        visibleSlotIndexes = visibleSlotIndexes,
        tilePreset = TilePresets.byId(tilePresetId),
        maxSamples = maxSamples,
    )

    private fun buildSignature(
        samples: List<TestLogSample>,
        renderSamples: List<TestLogSample>,
        metricMode: TestMetricMode,
        visibleSlotIndexes: Set<Int>,
        tilePresetKey: String,
    ): String = buildString {
        append(tilePresetKey)
        append('|')
        append(metricMode.name)
        append('|')
        append(visibleSlotIndexes.toSortedSet().joinToString(","))
        append('|')
        append(samples.size)
        append('|')
        append(renderSamples.size)
        append('|')
        append(samples.lastOrNull()?.timestampMillis ?: -1L)
        append('|')
        append(sampleSummaryHash(renderSamples))
    }

    private fun sampleSummaryHash(samples: List<TestLogSample>): Long {
        var hash = 1125899906842597L
        samples.forEach { sample ->
            hash = (hash xor sample.timestampMillis) * 1099511628211L
            hash = (hash xor sample.point?.longitude?.toBits().orZero()) * 1099511628211L
            hash = (hash xor sample.point?.latitude?.toBits().orZero()) * 1099511628211L
            sample.sims.forEach { sim ->
                hash = (hash xor sim.slotIndex.toLong()) * 1099511628211L
                val serving = sim.servingCell
                hash = (hash xor (serving?.rsrp ?: Int.MIN_VALUE).toLong()) * 1099511628211L
                hash = (hash xor (serving?.sinr ?: Int.MIN_VALUE).toLong()) * 1099511628211L
            }
        }
        return hash
    }

    private fun Long?.orZero(): Long = this ?: 0L

    private fun List<TestLogSample>.decimate(maxSamples: Int): List<TestLogSample> {
        if (maxSamples <= 0) return emptyList()
        if (size <= maxSamples) return this
        val step = kotlin.math.ceil(size.toDouble() / maxSamples.toDouble()).toInt().coerceAtLeast(1)
        val result = ArrayList<TestLogSample>(maxSamples)
        for (index in indices step step) {
            result += this[index]
            if (result.size >= maxSamples) break
        }
        if (result.lastOrNull()?.timestampMillis != last().timestampMillis && result.isNotEmpty()) {
            result[result.lastIndex] = last()
        }
        return result
    }

    private const val DEFAULT_MAX_SAMPLES = Int.MAX_VALUE
}
