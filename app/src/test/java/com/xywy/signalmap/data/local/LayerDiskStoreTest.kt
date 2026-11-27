package com.xywy.signalmap.data.local

import com.xywy.signalmap.domain.model.ImportedLayerRenderMode
import com.xywy.signalmap.domain.model.LayerDocument
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.LayerPointIconPreset
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.RasterLayerInfo
import com.xywy.signalmap.domain.model.SectorRecord
import kotlinx.serialization.json.Json
import org.junit.After
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertFails

class LayerDiskStoreTest {
    @Test
    fun `completed layer survives missing or corrupt index`() {
        val disk = LayerDiskStore(rootDir, json)
        val meta = metadata("orphan", true, true)
        val document = LayerDocument(meta, listOf(record(meta.id)))
        disk.saveLayer(document)
        rootDir.resolve("index.json.tmp").writeText("[")
        assertEquals(listOf(meta), disk.loadMetadata())
        rootDir.resolve("index.json").writeText("[broken")
        assertEquals(listOf(meta), disk.loadMetadata())
        assertEquals(document, disk.loadLayer(disk.loadMetadata().single()))
    }

    @Test
    fun `orphan recovery keeps indexed style and does not restore deleted layers`() {
        val disk = LayerDiskStore(rootDir, json)
        val original = metadata("existing", true, true)
        val styled = original.copy(isVisible = false, labelTextColor = "#00FF00")
        val orphan = metadata("new", true, true)
        val deleted = metadata("deleted", true, true)
        disk.saveLayer(LayerDocument(original, listOf(record(original.id))))
        disk.saveLayer(LayerDocument(orphan, emptyList()))
        disk.saveLayer(LayerDocument(deleted, emptyList()))
        disk.saveIndex(listOf(styled, deleted))
        disk.deleteLayer(deleted.id)
        assertEquals(setOf(styled, orphan), disk.loadMetadata().toSet())
    }

    @Test
    fun `delete failure is reported without removing files`() {
        val disk = LayerDiskStore(rootDir, json)
        val blocked = rootDir.resolve("blocked.json").apply { mkdirs() }
        blocked.resolve("child").writeText("preserved")
        assertFails { disk.deleteLayer("blocked") }
        assertEquals("preserved", blocked.resolve("child").readText())
    }

    @Test
    fun `failed replacement leaves the previous layer file readable`() {
        val disk = LayerDiskStore(rootDir, json)
        val meta = metadata("preserve", true, true)
        val original = LayerDocument(meta, listOf(record(meta.id)))
        disk.saveLayer(original)
        disk.saveIndex(listOf(meta))
        assertFails { disk.saveLayer(original.copy(sectors = listOf(record(meta.id).copy(longitude = Double.NaN)))) }
        assertEquals(original, disk.loadLayer(meta))
        assertEquals(listOf(meta), disk.loadMetadata())
        assertFalse(rootDir.resolve("preserve.json.tmp").exists())
    }

    private val rootDir = Files.createTempDirectory("layer-disk-store-test").toFile()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @After
    fun tearDown() {
        rootDir.deleteRecursively()
    }

    @Test
    fun `load layer applies latest metadata from index without rewriting layer file`() {
        val diskStore = LayerDiskStore(rootDir = rootDir, json = json)
        val originalMeta = metadata(layerId = "layer-a", isVisible = true, showLabels = true)
        val storedDocument = LayerDocument(
            meta = originalMeta,
            sectors = listOf(record(layerId = originalMeta.id)),
        )
        diskStore.saveIndex(listOf(originalMeta))
        diskStore.saveLayer(storedDocument)

        val currentMeta = originalMeta.copy(
            isVisible = false,
            showLabels = false,
            labelTextColor = "#00FF00",
            labelHaloColor = "#000000",
            pointIconPreset = LayerPointIconPreset.BLUE_PIN,
        )

        val loaded = diskStore.loadLayer(currentMeta)

        assertNotNull(loaded)
        assertEquals(1, loaded.sectors.size)
        assertFalse(loaded.meta.isVisible)
        assertFalse(loaded.meta.showLabels)
        assertEquals("#00FF00", loaded.meta.labelTextColor)
        assertEquals(LayerPointIconPreset.BLUE_PIN, loaded.meta.pointIconPreset)
    }

    @Test
    fun `load metadata keeps raster layer info for tif overlays`() {
        val diskStore = LayerDiskStore(rootDir = rootDir, json = json)
        val rasterMeta = metadata(
            layerId = "raster-layer",
            isVisible = true,
            showLabels = false,
        ).copy(
            sourceFileName = "demo.zip",
            recordCount = 762,
            rasterInfo = RasterLayerInfo(
                width = 684,
                height = 713,
                validPixelCount = 762,
                colorHex = "#FF4D4F",
                opacity = 0.45f,
                topLeft = com.xywy.signalmap.domain.model.GeoPoint(107.7, 35.4),
                topRight = com.xywy.signalmap.domain.model.GeoPoint(121.0, 35.4),
                bottomRight = com.xywy.signalmap.domain.model.GeoPoint(121.0, 34.2),
                bottomLeft = com.xywy.signalmap.domain.model.GeoPoint(107.7, 34.2),
            ),
        )

        diskStore.saveLayer(LayerDocument(rasterMeta, emptyList()))
        diskStore.saveIndex(listOf(rasterMeta))

        val reloaded = diskStore.loadMetadata().single()

        assertEquals("#FF4D4F", reloaded.rasterInfo?.colorHex)
        assertEquals(0.45f, reloaded.rasterInfo?.opacity)
        assertEquals(684, reloaded.rasterInfo?.width)
        assertEquals(762, reloaded.rasterInfo?.validPixelCount)
    }

    private fun metadata(
        layerId: String,
        isVisible: Boolean,
        showLabels: Boolean,
    ) = LayerMetadata(
        id = layerId,
        name = "测试图层",
        sourceFileName = "test.xlsx",
        importedAtMillis = 123L,
        isVisible = isVisible,
        recordCount = 1,
        sheetName = "LTE",
        renderMode = ImportedLayerRenderMode.AUTO,
        detailFieldHeaders = listOf("站高"),
        showLabels = showLabels,
        labelTextColor = "#FFFFFF",
        labelHaloColor = "#111111",
        pointIconPreset = LayerPointIconPreset.RED_PIN,
    )

    private fun record(layerId: String) = SectorRecord(
        id = "record-1",
        layerId = layerId,
        radioTechnology = RadioTechnology.LTE,
        siteId = "site-1",
        siteName = "测试站点",
        cellId = "1001",
        longitude = 120.8,
        latitude = 34.4,
        rawFields = mapOf("站高" to "30"),
    )
}
