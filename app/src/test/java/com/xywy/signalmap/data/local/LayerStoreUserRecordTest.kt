package com.xywy.signalmap.data.local

import android.content.ContextWrapper
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.junit.After
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertFails

class LayerStoreUserRecordTest {
    @Test
    fun `failed layer deletion keeps the item visible for retry`() = runTest {
        val layer = store.createUserLayer("待删除", "blocked-delete")
        val file = File(rootDir, "layers/blocked-delete.json")
        val saved = File(rootDir, "layers/blocked-delete.backup")
        java.nio.file.Files.move(file.toPath(), saved.toPath())
        file.mkdirs()
        File(file, "blocked").writeText("keep")
        assertFails { store.deleteLayer(layer.meta.id) }
        assertEquals(listOf(layer.meta.id), store.documents.value.map { it.meta.id })
    }

    @Test
    fun `full rendering switch defaults off and persists without dropping hidden records`() = runTest {
        val layer = store.createUserLayer("范围切换", "render-mode")
        assertFalse(layer.meta.alwaysRenderAllRecords)
        store.upsertUserRecord(layer.meta.id, userPoint(layer.meta.id, "one", "保留数据"))
        store.setVisibility(layer.meta.id, false)
        val disk = LayerDiskStore(File(rootDir, "layers"), Json { ignoreUnknownKeys = true })
        for (enabled in listOf(true, false)) {
            store.setAlwaysRenderAllRecords(layer.meta.id, enabled)
            val metadata = disk.loadMetadata().single()
            assertEquals(enabled, metadata.alwaysRenderAllRecords)
            assertEquals(1, disk.loadLayer(metadata)?.sectors?.size)
            assertFalse(metadata.isVisible)
            assertEquals(0, store.documents.value.single().sectors.size)
        }
        store.setVisibility(layer.meta.id, true)
        assertEquals(1, store.documents.value.single().sectors.size)
        assertFalse(store.documents.value.single().meta.alwaysRenderAllRecords)
    }

    @Test
    fun `concurrent additions and visibility changes keep disk and memory consistent`() = runTest {
        val layer = store.createUserLayer("并发写入", "concurrent-layer")
        coroutineScope {
            repeat(24) { index ->
                launch { store.upsertUserRecord(layer.meta.id, userPoint(layer.meta.id, "point-$index", "点$index")) }
                launch { store.setVisibility(layer.meta.id, index % 2 == 0) }
            }
        }
        store.setVisibility(layer.meta.id, false)
        val expectedIds = (0 until 24).map { "point-$it" }.toSet()
        assertEquals(expectedIds, store.findLayer(layer.meta.id)?.sectors?.map { it.id }?.toSet())
        assertEquals(0, store.documents.value.single().sectors.size)
        val disk = LayerDiskStore(File(rootDir, "layers"), Json { ignoreUnknownKeys = true })
        val meta = disk.loadMetadata().single()
        assertEquals(24, meta.recordCount)
        assertFalse(meta.isVisible)
        assertEquals(expectedIds, disk.loadLayer(meta)?.sectors?.map { it.id }?.toSet())
        store.setVisibility(layer.meta.id, true)
        assertEquals(24, store.documents.value.single().sectors.size)
    }

    @Test
    fun `adding to a hidden layer preserves existing records on disk`() = runTest {
        val layer = store.createUserLayer("隐藏图层", "hidden-add")
        store.upsertUserRecord(layer.meta.id, userPoint(layer.meta.id, "first", "已有点"))
        store.setVisibility(layer.meta.id, false)
        assertEquals(0, store.documents.value.single().sectors.size)
        store.upsertUserRecord(layer.meta.id, userPoint(layer.meta.id, "second", "新增点"))
        val saved = assertNotNull(store.findLayer(layer.meta.id))
        assertEquals(setOf("first", "second"), saved.sectors.map { it.id }.toSet())
        assertFalse(saved.meta.isVisible)
        assertEquals(0, store.documents.value.single().sectors.size)
        store.setVisibility(layer.meta.id, true)
        assertEquals(2, store.documents.value.single().sectors.size)
    }

    @Test
    fun `editing and deleting hidden records uses full saved layer`() = runTest {
        val layer = store.createUserLayer("隐藏图层", "hidden-edit")
        store.upsertUserRecord(layer.meta.id, userPoint(layer.meta.id, "first", "待编辑"))
        store.upsertUserRecord(layer.meta.id, userPoint(layer.meta.id, "second", "保留"))
        store.setVisibility(layer.meta.id, false)
        assertNotNull(store.updateUserRecord(layer.meta.id, "first", "已编辑"))
        assertEquals("已编辑", store.findLayer(layer.meta.id)?.sectors?.first { it.id == "first" }?.siteName)
        assertNotNull(store.deleteRecord(layer.meta.id, "first"))
        assertEquals(listOf("second"), store.findLayer(layer.meta.id)?.sectors?.map { it.id })
        assertEquals(0, store.documents.value.single().sectors.size)
    }

    private val rootDir = Files.createTempDirectory("layer-store-user-record-test").toFile()
    private val context = object : ContextWrapper(null) {
        override fun getFilesDir(): File = rootDir
    }
    private val store = LayerStore(context)

    @After
    fun tearDown() {
        rootDir.deleteRecursively()
    }

    @Test
    fun `updating a missing user record returns null and leaves layer unchanged`() = runTest {
        val layer = store.createUserLayer(name = "手动图层", layerId = "layer-a")
        store.upsertUserRecord(
            layerId = layer.meta.id,
            record = userPoint(
                layerId = layer.meta.id,
                recordId = "point-1",
                name = "原始点",
            ),
        )

        val updated = store.updateUserRecord(
            layerId = layer.meta.id,
            recordId = "missing-point",
            name = "不应写入",
            longitude = 121.0,
            latitude = 35.0,
        )

        val reloaded = store.findLayer(layer.meta.id)
        assertNull(updated)
        assertEquals(1, reloaded?.sectors?.size)
        assertEquals("原始点", reloaded?.sectors?.single()?.cellName)
        assertEquals(120.8, reloaded?.sectors?.single()?.longitude)
    }

    @Test
    fun `deleting a missing user record returns null and keeps record count`() = runTest {
        val layer = store.createUserLayer(name = "手动图层", layerId = "layer-b")
        store.upsertUserRecord(
            layerId = layer.meta.id,
            record = userPoint(
                layerId = layer.meta.id,
                recordId = "point-1",
                name = "保留点",
            ),
        )

        val updated = store.deleteRecord(layerId = layer.meta.id, recordId = "missing-point")

        val reloaded = store.findLayer(layer.meta.id)
        assertNull(updated)
        assertEquals(1, reloaded?.meta?.recordCount)
        assertEquals("保留点", reloaded?.sectors?.single()?.cellName)
    }

    private fun userPoint(
        layerId: String,
        recordId: String,
        name: String,
    ): SectorRecord = SectorRecord(
        id = recordId,
        layerId = layerId,
        radioTechnology = RadioTechnology.UNKNOWN,
        siteName = name,
        cellName = name,
        longitude = 120.8,
        latitude = 34.4,
        isPointOnly = true,
    )
}
