package com.xywy.signalmap.data.local

import android.content.ContextWrapper
import com.xywy.signalmap.domain.model.TestMetricMode
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class TestLogStorePublicationTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun `index write failure does not publish visibility or a new log`() = runTest {
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = temporary.root
        }
        val store = TestLogStore(context)
        val original = store.addRecordedLog("first", "first.xywy", 1, 2, emptyList(), TestMetricMode.RSRP)
        val pendingIndex = File(temporary.root, "test-logs/index.json.tmp").apply { mkdirs() }
        File(pendingIndex, "block").writeText("keep")
        assertFails { store.setVisibility(original.meta.id, false) }
        assertTrue(store.documents.value.single().meta.isVisible)
        assertFails { store.addRecordedLog("second", "second.xywy", 3, 4, emptyList(), TestMetricMode.RSRP) }
        assertEquals(listOf(original.meta.id), store.documents.value.map { it.meta.id })
    }
}
