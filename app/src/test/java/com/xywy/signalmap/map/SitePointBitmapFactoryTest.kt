package com.xywy.signalmap.map

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.xywy.signalmap.domain.model.LayerPointIconCatalog
import com.xywy.signalmap.domain.model.LayerPointIconPreset
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SitePointBitmapFactoryTest {
    @Test
    fun `every point preset draws visible colored pixels at phone densities`() {
        val application = RuntimeEnvironment.getApplication()
        listOf(160, 320, 480).forEach { dpi ->
            val configuration = Configuration(application.resources.configuration).apply { densityDpi = dpi }
            val context = application.createConfigurationContext(configuration)
            LayerPointIconPreset.entries.forEach { preset ->
                val bitmap = SitePointBitmapFactory.create(context, preset)
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                assertTrue(bitmap.config == Bitmap.Config.ARGB_8888)
                assertEquals(dpi, bitmap.density)
                assertEquals((SiteSymbolLayout.POINT_SIZE_DP * dpi / 160f).toInt(), bitmap.width)
                assertTrue(pixels.count { Color.alpha(it) > 0 } > pixels.size / 10, "$preset at $dpi has no visible shape")
                val fill = Color.parseColor(LayerPointIconCatalog.style(preset).fillColor)
                assertTrue(pixels.any { it == fill }, "$preset at $dpi lost its fill color")
            }
        }
    }

    @Test
    fun `style image catalog covers every feature icon id`() {
        val images = SitePointBitmapFactory.createStyleImages(RuntimeEnvironment.getApplication())
        assertEquals(LayerPointIconPreset.entries.map { it.iconId }.toSet(), images.keys)
        assertTrue(images.values.all { !it.isRecycled && it.width > 0 })
    }

    @Test
    fun `labels leave all point icon pixels visible`() {
        val application = RuntimeEnvironment.getApplication()
        val context = application.createConfigurationContext(
            Configuration(application.resources.configuration).apply { densityDpi = 160 },
        )
        val canvasBitmap = Bitmap.createBitmap(560, 310, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)
        canvas.drawColor(Color.rgb(228, 235, 243))
        LayerPointIconPreset.entries.forEachIndexed { index, preset ->
            val icon = SitePointBitmapFactory.create(context, preset)
            val label = SiteLabelBitmapFactory.create(context, preset.title, "#111111", "#FFFFFF")
            val centerX = 100 + (index % 3) * 180
            val centerY = 65 + (index / 3) * 150
            val iconX = centerX - icon.width / 2
            val iconY = centerY - icon.height / 2
            canvas.drawBitmap(icon, iconX.toFloat(), iconY.toFloat(), null)
            val before = IntArray(icon.width * icon.height)
            canvasBitmap.getPixels(before, 0, icon.width, iconX, iconY, icon.width, icon.height)
            val labelTop = centerY + SiteSymbolLayout.LABEL_OFFSET_Y_DP
            assertEquals("top", SiteSymbolLayout.LABEL_ANCHOR)
            assertTrue(labelTop >= centerY + icon.height / 2f + SiteSymbolLayout.LABEL_GAP_DP)
            canvas.drawBitmap(label, centerX - label.width / 2f, labelTop, null)
            val after = IntArray(before.size)
            canvasBitmap.getPixels(after, 0, icon.width, iconX, iconY, icon.width, icon.height)
            assertTrue(before.contentEquals(after), "Label obscured $preset")
        }
        val output = File("build/reports/point-icon-review").apply { mkdirs() }
        File(output, "all-presets.png").outputStream().use { canvasBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
