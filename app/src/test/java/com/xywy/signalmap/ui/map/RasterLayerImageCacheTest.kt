package com.xywy.signalmap.ui.map

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class RasterLayerImageCacheTest {
    @Test
    fun `style changes invalidate equal size files with the same timestamp`() {
        val dir = Files.createTempDirectory("xywy-raster-revision").toFile()
        try {
            val file = File(dir, "layer.png").apply { writeText("red") }
            val timestamp = file.lastModified()
            var loads = 0
            val cache = RasterLayerImageCache<String> { loads++; it.readText() }
            assertEquals("red", cache.get(file, "style-red"))
            file.writeText("new")
            check(file.setLastModified(timestamp))
            assertEquals("new", cache.get(file, "style-green"))
            assertEquals("new", cache.get(file, "style-green"))
            assertEquals(2, loads)
        } finally { dir.deleteRecursively() }
    }

    @Test
    fun `cache reuses loaded value until file changes`() {
        val dir = Files.createTempDirectory("xywy-raster-cache").toFile()
        val file = File(dir, "layer.png")
        file.writeText("one")
        var loads = 0
        val cache = RasterLayerImageCache(maxEntries = 2) { candidate ->
            loads += 1
            candidate.readText()
        }

        assertEquals("one", cache.get(file))
        assertEquals("one", cache.get(file))
        file.writeText("two")

        assertEquals("two", cache.get(file))
        assertEquals(2, loads)
    }

    @Test
    fun `cache trims least recently used entries`() {
        val dir = Files.createTempDirectory("xywy-raster-cache-lru").toFile()
        val a = File(dir, "a.png").apply { writeText("a") }
        val b = File(dir, "b.png").apply { writeText("b") }
        val c = File(dir, "c.png").apply { writeText("c") }
        var loads = 0
        val cache = RasterLayerImageCache(maxEntries = 2) { candidate ->
            loads += 1
            candidate.nameWithoutExtension
        }

        assertEquals("a", cache.get(a))
        assertEquals("b", cache.get(b))
        assertEquals("a", cache.get(a))
        assertEquals("c", cache.get(c))
        assertEquals("b", cache.get(b))

        assertEquals(4, loads)
    }
}
