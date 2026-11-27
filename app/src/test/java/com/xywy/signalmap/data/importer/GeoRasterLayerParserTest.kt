package com.xywy.signalmap.data.importer

import com.xywy.signalmap.domain.model.TileCoordinateSystem
import mil.nga.tiff.FieldTagType
import mil.nga.tiff.FieldType
import mil.nga.tiff.FileDirectory
import mil.nga.tiff.Rasters
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffWriter
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GeoRasterLayerParserTest {
    @Test
    fun `generated geotiff retains bounds and nodata mask`() {
        val parsed = GeoRasterLayerParser().parseTiff(sampleTiff().inputStream(), "sample.tif")
        assertEquals(2, parsed.width)
        assertEquals(2, parsed.height)
        assertEquals(2, parsed.validPixelCount)
        assertContentEquals(byteArrayOf(0, -1, -1, 0), parsed.alphaMask)
        assertNull(parsed.coordinateSystem)
        assertEquals(120.0, parsed.bounds.minLongitude, 1e-6)
        assertEquals(120.02, parsed.bounds.maxLongitude, 1e-6)
        assertEquals(33.98, parsed.bounds.minLatitude, 1e-6)
        assertEquals(34.0, parsed.bounds.maxLatitude, 1e-6)
    }

    @Test
    fun `zip sidecars are matched within the tiff directory`() {
        val archive = zip(
            "other/sample.tfw" to "1\n0\n0\n-1\n0\n0".toByteArray(),
            "folder/SAMPLE.TFW" to "0.01\n0\n0\n-0.01\n120.005\n33.995".toByteArray(),
            "folder/sample.prj" to "GCJ02".toByteArray(),
            "folder/sample.tif" to sampleTiff(),
            "unrelated.txt" to ByteArray(8192),
        )
        val parsed = GeoRasterLayerParser().parseZip(archive.inputStream(), "sample.zip")
        assertEquals(TileCoordinateSystem.GCJ02, parsed.coordinateSystem)
        assertEquals(2, parsed.validPixelCount)
        assertEquals(120.0, parsed.bounds.minLongitude, 1e-6)
        assertEquals(34.0, parsed.bounds.maxLatitude, 1e-6)
    }

    @Test
    fun `projected or nonfinite bounds are rejected before rendering`() {
        for (origin in listOf(500_000.0, Double.NaN)) {
            assertFailsWith<IllegalArgumentException> {
                GeoRasterLayerParser().parseTiff(sampleTiff(origin).inputStream(), "invalid.tif")
            }
        }
    }

    @Test
    fun `oversized sidecar is rejected while decompressing`() {
        val archive = zip("sample.tfw" to ByteArray(65_537))
        assertFailsWith<IllegalArgumentException> {
            GeoRasterLayerParser().parseZip(archive.inputStream(), "large.zip")
        }
    }

    private fun sampleTiff(origin: Double = 120.0): ByteArray {
        val rasters = Rasters(2, 2, 1, FieldType.BYTE).apply {
            setFirstPixelSample(0, 0, 0)
            setFirstPixelSample(1, 0, 1)
            setFirstPixelSample(0, 1, 2)
            setFirstPixelSample(1, 1, 0)
        }
        val directory = FileDirectory(rasters).apply {
            setImageWidth(2)
            setImageHeight(2)
            setBitsPerSample(8)
            setSamplesPerPixel(1)
            setCompression(1)
            setPhotometricInterpretation(1)
            setPlanarConfiguration(1)
            setRowsPerStrip(2)
            setSampleFormat(1)
            setModelPixelScale(listOf(0.01, 0.01, 0.0))
            setModelTiepoint(listOf(0.0, 0.0, 0.0, origin, 34.0, 0.0))
            setStringEntryValue(FieldTagType.GDAL_NODATA, "0")
        }
        return TiffWriter.writeTiffToBytes(TIFFImage(directory))
    }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { output ->
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }.toByteArray()
}
