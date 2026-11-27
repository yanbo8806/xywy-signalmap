package com.xywy.signalmap.data.importer

import kotlin.test.Test
import kotlin.test.assertEquals

class ImportFileFormatResolverTest {
    @Test
    fun `resolver recognizes xywy archive by extension and mime`() {
        assertEquals(
            ImportFileFormat.XYWY,
            ImportFileFormatResolver.resolve(
                displayName = "route.xywy",
                mimeType = "application/x-xywy-log",
            ),
        )
        assertEquals(
            ImportFileFormat.XYWY,
            ImportFileFormatResolver.resolve(displayName = "route.xywy"),
        )
    }

    @Test
    fun `resolver recognizes tif tiff and zip package imports`() {
        assertEquals(
            ImportFileFormat.TIFF,
            ImportFileFormatResolver.resolve(
                displayName = "sample.tif",
                mimeType = "image/tiff",
            ),
        )
        assertEquals(
            ImportFileFormat.TIFF,
            ImportFileFormatResolver.resolve(displayName = "sample.tiff"),
        )
        assertEquals(
            ImportFileFormat.TIFF_ZIP,
            ImportFileFormatResolver.resolve(
                displayName = "sample.zip",
                mimeType = "application/zip",
            ),
        )
    }
}
