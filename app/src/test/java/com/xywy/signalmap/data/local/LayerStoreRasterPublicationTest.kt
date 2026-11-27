package com.xywy.signalmap.data.local

import android.app.Application
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.graphics.Color
import com.xywy.signalmap.data.importer.GeoImageBounds
import com.xywy.signalmap.data.importer.ParsedRasterLayer
import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.LayerPointIconPreset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayerStoreRasterPublicationTest {
    @Test
    fun `new raster style is published only after the image has the new color`() = runBlocking {
        val root = Files.createTempDirectory("xywy-raster-publication").toFile()
        val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun getFilesDir(): File = root
        }
        val store = LayerStore(context)
        val observed = CompletableDeferred<Int>()
        var observation: kotlinx.coroutines.Job? = null
        try {
            store.addRasterLayer(
                "测试栅格", "test.tif",
                ParsedRasterLayer(
                    width = 2, height = 2, alphaMask = ByteArray(4) { 0xff.toByte() }, validPixelCount = 4,
                    bounds = GeoImageBounds(GeoPoint(120.8, 34.5), GeoPoint(120.9, 34.5), GeoPoint(120.9, 34.4), GeoPoint(120.8, 34.4)),
                ), layerId = "raster",
            )
            val render = RasterLayerAssetPaths.renderFile(File(root, "layers"), "raster")
            observation = launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
                store.documents.collect { documents ->
                    val raster = documents.singleOrNull()?.meta?.rasterInfo
                    if (raster?.colorHex == "#00FF00") {
                        val bitmap = assertNotNull(BitmapFactory.decodeFile(render.absolutePath))
                        try { observed.complete(bitmap.getPixel(0, 0)) } finally { bitmap.recycle() }
                    }
                }
            }
            store.updateStyle(
                "raster", false, null, null, LayerPointIconPreset.RED_PIN,
                rasterColorHex = "#00FF00", rasterOpacity = 1f,
            )
            assertEquals(Color.GREEN, withTimeout(5000) { observed.await() })
            assertEquals("#00FF00", store.findLayer("raster")?.meta?.rasterInfo?.colorHex)
        } finally {
            observation?.cancelAndJoin()
            root.deleteRecursively()
        }
    }
}
