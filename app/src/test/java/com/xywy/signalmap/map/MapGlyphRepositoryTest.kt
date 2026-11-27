package com.xywy.signalmap.map

import android.app.Application
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MapGlyphRepositoryTest {
    @Test
    fun `station numbers latin letters and punctuation are bundled and cached offline`() {
        val context = RuntimeEnvironment.getApplication()
        val repository = MapGlyphRepository(context)
        listOf("0-255", "256-511", "768-1023", "8192-8447", "8448-8703", "8704-8959", "12288-12543", "65280-65535").forEach { range ->
            val raw = context.assets.open("map-fonts/$range.pbf").use { it.readBytes() }
            assertTrue(raw.size > 1000)
            val glyphs = assertNotNull(repository.getGlyphs("Noto Sans Regular", range))
            assertTrue(raw.contentEquals(glyphs.bytes))
            assertSame(glyphs.bytes, repository.getGlyphs("Noto Sans Regular", range)?.bytes)
        }
        assertNull(repository.getGlyphs("../outside", "0-255"))
        assertNull(repository.getGlyphs("Noto Sans Regular", "../../outside"))
    }
}
