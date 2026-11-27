package com.xywy.signalmap.data.importer

import kotlin.test.Test
import kotlin.test.assertEquals

class ImportFieldSuggesterTest {
    @Test
    fun `suggest maps common headers and prefills detail fields`() {
        val suggestion = ImportFieldSuggester.suggest(
            headers = listOf(
                "\u540d\u79f0",
                "\u7ecf\u5ea6",
                "\u7eac\u5ea6",
                "\u65b9\u4f4d\u89d2",
                "PCI",
                "\u7ad9\u9ad8",
                "\u57fa\u7ad9ID",
                "\u5907\u6ce8",
            ),
        )

        assertEquals("\u7ecf\u5ea6", suggestion.mapping.longitudeHeader)
        assertEquals("\u7eac\u5ea6", suggestion.mapping.latitudeHeader)
        assertEquals("\u65b9\u4f4d\u89d2", suggestion.mapping.azimuthHeader)
        assertEquals("\u540d\u79f0", suggestion.mapping.nameHeader)
        assertEquals("\u57fa\u7ad9ID", suggestion.mapping.siteIdHeader)
        assertEquals("PCI", suggestion.mapping.pciHeader)
        assertEquals("\u7ad9\u9ad8", suggestion.mapping.antennaHeightHeader)
        assertEquals(
            listOf("PCI", "\u7ad9\u9ad8", "\u57fa\u7ad9ID"),
            suggestion.defaultDetailHeaders,
        )
    }

    @Test
    fun `suggest prefers eci identity column over cgi and local id`() {
        val suggestion = ImportFieldSuggester.suggest(
            headers = listOf(
                "CGI(小区唯一标识)",
                "ECI（唯一标识列）",
                "LocalID",
                "经度",
                "纬度",
            ),
        )

        assertEquals("ECI（唯一标识列）", suggestion.mapping.cellIdHeader)
    }
}
