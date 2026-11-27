package com.xywy.signalmap.ui.map

import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapFeatureDetailsFormatterTest {
    @Test
    fun `detail rows include pinned import fields`() {
        val layer = LayerMetadata(
            id = "layer-1",
            name = "\u6d4b\u8bd5\u56fe\u5c42",
            sourceFileName = "demo.xlsx",
            importedAtMillis = 1L,
            isVisible = true,
            recordCount = 1,
            detailFieldHeaders = listOf("\u7ad9\u9ad8", "\u5907\u6ce8"),
        )
        val record = SectorRecord(
            id = "sector-1",
            layerId = "layer-1",
            radioTechnology = RadioTechnology.LTE,
            siteId = "SITE-001",
            siteName = "\u7ad9\u70b9A",
            pci = "321",
            longitude = 120.1234,
            latitude = 34.5678,
            rawFields = mapOf(
                "\u7ad9\u9ad8" to "35m",
                "\u5907\u6ce8" to "\u697c\u9876\u7ad9",
                "\u5382\u5bb6" to "\u534e\u4e3a",
            ),
        )

        val rows = MapFeatureDetailsFormatter.build(record, layer)
        val labels = rows.map { it.label }

        assertEquals("35m", rows.first { it.label == "\u7ad9\u9ad8" }.value)
        assertEquals("\u697c\u9876\u7ad9", rows.first { it.label == "\u5907\u6ce8" }.value)
        assertTrue("\u7ad9\u9ad8" in labels)
        assertTrue("\u5907\u6ce8" in labels)
        assertTrue("\u5382\u5bb6" !in labels)
    }

    @Test
    fun `detail rows do not append unselected raw fields`() {
        val layer = LayerMetadata(
            id = "layer-1",
            name = "\u6d4b\u8bd5\u56fe\u5c42",
            sourceFileName = "demo.xlsx",
            importedAtMillis = 1L,
            isVisible = true,
            recordCount = 1,
            detailFieldHeaders = listOf("\u7ad9\u9ad8"),
        )
        val record = SectorRecord(
            id = "sector-1",
            layerId = "layer-1",
            radioTechnology = RadioTechnology.LTE,
            siteName = "\u7ad9\u70b9A",
            longitude = 120.1234,
            latitude = 34.5678,
            rawFields = mapOf(
                "\u7ad9\u9ad8" to "35m",
            ),
        )

        val rows = MapFeatureDetailsFormatter.build(record, layer)
        val labels = rows.map { it.label }

        assertTrue("\u7ad9\u9ad8" in labels)
        assertTrue("\u5382\u5bb6" !in labels)
    }

    @Test
    fun `editable user point details only show name coordinate and note`() {
        val layer = LayerMetadata(
            id = "layer-1",
            name = "手动图层",
            sourceFileName = "manual",
            importedAtMillis = 1L,
            isVisible = true,
            recordCount = 1,
            userEditable = true,
        )
        val record = SectorRecord(
            id = "point-1",
            layerId = "layer-1",
            radioTechnology = RadioTechnology.UNKNOWN,
            siteName = "点A",
            cellName = "点A",
            longitude = 120.1234567,
            latitude = 34.7654321,
            isPointOnly = true,
            rawFields = mapOf(
                "备注" to "临时标记",
                "类型" to "点",
                "图标" to "红色图钉",
            ),
        )

        val rows = MapFeatureDetailsFormatter.build(record, layer)

        assertEquals(
            listOf("名称", "经度", "纬度", "备注"),
            rows.map { it.label },
        )
        assertEquals("点A", rows[0].value)
        assertEquals("120.123457", rows[1].value)
        assertEquals("34.765432", rows[2].value)
        assertEquals("临时标记", rows[3].value)
    }
}
