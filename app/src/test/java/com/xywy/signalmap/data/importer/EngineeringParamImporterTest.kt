package com.xywy.signalmap.data.importer

import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.ImportedLayerRenderMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EngineeringParamImporterTest {
    @Test
    fun `out of range and non-finite coordinates are skipped instead of creating invisible features`() {
        val rows = listOf(
            "120.8" to "34.4", "NaN" to "34.4", "Infinity" to "34.4",
            "208.8" to "34.4", "120.8" to "134.4",
        ).map { (lon, lat) -> mapOf("名称" to "test", "经度" to lon, "纬度" to lat) }
        val parsed = EngineeringParamImporter().parseSheets(listOf(ParsedSheet("Sheet1", rows)), "coordinates")
        assertEquals(1, parsed.records.size)
        assertEquals(4, parsed.skippedCount)
    }

    @Test
    fun `import format resolver treats sheet config as xlsx even without file extension`() {
        val format = ImportFileFormatResolver.resolve(
            displayName = "wechat-temp-file",
            mimeType = null,
            sheetConfig = ImportSheetConfig(selectedSheetName = "sheet1"),
        )

        assertEquals(ImportFileFormat.XLSX, format)
    }

    @Test
    fun `import format resolver keeps kml and unknown paths distinct`() {
        assertEquals(
            ImportFileFormat.KML,
            ImportFileFormatResolver.resolve(displayName = "demo.kml"),
        )
        assertEquals(
            ImportFileFormat.UNKNOWN,
            ImportFileFormatResolver.resolve(displayName = "wechat-temp-file"),
        )
    }

    @Test
    fun `import format resolver can fall back to mime type for xlsx and kml`() {
        assertEquals(
            ImportFileFormat.XLSX,
            ImportFileFormatResolver.resolve(
                displayName = "wechat-temp-file",
                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            ),
        )
        assertEquals(
            ImportFileFormat.KML,
            ImportFileFormatResolver.resolve(
                displayName = "wechat-temp-file",
                mimeType = "application/vnd.google-earth.kml+xml",
            ),
        )
    }

    @Test
    fun `parse sheets imports lte and nr rows from workbook style headers`() {
        val importer = EngineeringParamImporter()

        val result = importer.parseSheets(
            sheets = listOf(
                ParsedSheet(
                    name = "LTE",
                    rows = listOf(
                        mapOf(
                            "地市" to "示例地区",
                            "网元名称" to "示例地区示范城示例站点-ZLH-DEMO-LTE-01",
                            "基站编号" to "DEMO-LTE-01",
                            "ECI（唯一标识列）" to "64076",
                            "小区名称" to "示例地区示范城示例站点-ZLH-DEMO-LTE-01-0",
                            "经度" to "120.75686",
                            "纬度" to "34.37036",
                            "方位角（度）" to "58",
                            "TAC(十进制)" to "37122",
                            "频带指示" to "8",
                            "小区带宽" to "10M",
                            "PCI" to "336",
                            "设备厂家" to "中兴",
                        ),
                    ),
                ),
                ParsedSheet(
                    name = "NR",
                    rows = listOf(
                        mapOf(
                            "地市" to "示例地区",
                            "网元名称" to "示例地区示例县yx-ZNH-DEMO-NR-01",
                            "基站编号" to "DEMO-NR-01",
                            "ECI（唯一标识列）" to "81940",
                            "小区名称" to "示例地区示例县yx-ZNH-DEMO-NR-01-0",
                            "经度" to "120.08222",
                            "纬度" to "35.03611",
                            "方位角（度）" to "60",
                            "TAC(十进制)" to "1235",
                            "频带指示" to "n41",
                            "小区带宽" to "100M",
                            "PCI" to "558",
                            "设备厂家" to "中兴",
                        ),
                    ),
                ),
            ),
            layerId = "layer-1",
        )

        assertEquals(2, result.records.size)

        val lte = result.records.first { it.radioTechnology == RadioTechnology.LTE }
        assertEquals("DEMO-LTE-01", lte.siteId)
        assertEquals("示例地区示范城示例站点-ZLH-DEMO-LTE-01", lte.siteName)
        assertEquals("64076", lte.cellId)
        assertEquals("B8", lte.band)
        assertEquals("10 MHz", lte.bandwidth)

        val nr = result.records.first { it.radioTechnology == RadioTechnology.NR }
        assertEquals("DEMO-NR-01", nr.siteId)
        assertEquals("81940", nr.cellId)
        assertEquals("N41", nr.band)
        assertEquals("100 MHz", nr.bandwidth)
    }

    @Test
    fun `parse sheets prefers eci or nci over cgi and local cell id for matching`() {
        val importer = EngineeringParamImporter()

        val result = importer.parseSheets(
            sheets = listOf(
                ParsedSheet(
                    name = "LTE",
                    rows = listOf(
                        linkedMapOf(
                            "CGI(小区唯一标识)" to "001-01-000250-76",
                            "ECI（唯一标识列）" to "64076",
                            "LocalID" to "76",
                            "小区名称" to "示例地区测试LTE-0",
                            "经度" to "120.76032",
                            "纬度" to "34.37364",
                            "PCI" to "120",
                        ),
                    ),
                ),
            ),
            layerId = "layer-identity",
        )

        assertEquals("64076", result.records.single().cellId)
    }

    @Test
    fun `parse sheets keeps eci unique identity when field mapping points at cgi`() {
        val importer = EngineeringParamImporter()

        val result = importer.parseSheets(
            sheets = listOf(
                ParsedSheet(
                    name = "NR",
                    rows = listOf(
                        linkedMapOf(
                            "网元名称" to "示例地区示范区数据中心办公楼9至12F-HNW-DEMO-NR-02",
                            "基站编号" to "DEMO-NR-02",
                            "gNODEB ID" to "20",
                            "CGI(小区唯一标识)" to "001-01-000020-20",
                            "ECI（唯一标识列）" to "TEST-NCI-01",
                            "小区名称" to "示例地区示范区数据中心办公楼9至12F-HNW-DEMO-NR-02-0",
                            "NR DU小区ID" to "20",
                            "经度" to "120.704431",
                            "纬度" to "34.29489",
                            "PCI" to "697",
                        ),
                    ),
                ),
            ),
            layerId = "layer-identity",
            options = ImportOptions(
                sheetConfig = ImportSheetConfig(
                    selectedSheetName = "NR",
                    fieldMapping = ImportFieldMapping(
                        longitudeHeader = "经度",
                        latitudeHeader = "纬度",
                        nameHeader = "小区名称",
                        siteIdHeader = "基站编号",
                        cellIdHeader = "CGI(小区唯一标识)",
                        pciHeader = "PCI",
                    ),
                ),
            ),
        )

        val record = result.records.single()
        assertEquals("TEST-NCI-01", record.cellId)
        assertEquals("示例地区示范区数据中心办公楼9至12F-HNW-DEMO-NR-02-0", record.cellName)
    }

    @Test
    fun `parse sheets can convert gcj02 coordinates when enabled`() {
        val importer = EngineeringParamImporter()

        val result = importer.parseSheets(
            sheets = listOf(
                ParsedSheet(
                    name = "LTE",
                    rows = listOf(
                        mapOf(
                            "经度" to "120.75686",
                            "纬度" to "34.37036",
                            "频带指示" to "8",
                            "小区带宽" to "10M",
                        ),
                    ),
                ),
            ),
            layerId = "layer-2",
            options = ImportOptions(convertGcj02ToWgs84 = true),
        )

        val record = result.records.single()
        assertEquals("10 MHz", record.bandwidth)
        assertTrue(record.longitude in 120.70..120.76)
        assertTrue(record.latitude in 34.36..34.38)
        assertTrue(record.longitude != 120.75686)
        assertTrue(record.latitude != 34.37036)
    }


    @Test
    fun `parse sheets recognizes simple point rows and marks them as point only`() {
        val importer = EngineeringParamImporter()

        val result = importer.parseSheets(
            sheets = listOf(
                ParsedSheet(
                    name = "POI",
                    rows = listOf(
                        mapOf(
                            "名称" to "观测点A",
                            "经度" to "120.8001",
                            "纬度" to "34.4002",
                        ),
                    ),
                ),
            ),
            layerId = "layer-point",
        )

        assertEquals(1, result.records.size)
        val record = result.records.single()
        assertEquals(RadioTechnology.UNKNOWN, record.radioTechnology)
        assertEquals("观测点A", record.siteName)
        assertEquals("观测点A", record.searchText)
        assertTrue(record.isPointOnly)
    }


    @Test
    fun `parse sheets can import one selected sheet with manual mapping and sector mode`() {
        val importer = EngineeringParamImporter()

        val result = importer.parseSheets(
            sheets = listOf(
                ParsedSheet(
                    name = "sheet1",
                    rows = listOf(
                        mapOf(
                            "\u540d\u79f0" to "\u65e7\u8868\u6570\u636e",
                            "\u7ecf\u5ea6" to "120.1",
                            "\u7eac\u5ea6" to "34.1",
                        ),
                    ),
                ),
                ParsedSheet(
                    name = "sheet2",
                    rows = listOf(
                        mapOf(
                            "\u7ad9\u70b9\u540d" to "\u5019\u9009\u6247\u533aA",
                            "lng" to "120.9123",
                            "lat" to "34.2789",
                            "bearing" to "45",
                            "site_no" to "SITE-2",
                            "pci_no" to "123",
                        ),
                    ),
                ),
            ),
            layerId = "layer-manual",
            options = ImportOptions(
                sheetConfig = ImportSheetConfig(
                    selectedSheetName = "sheet2",
                    renderMode = ImportedLayerRenderMode.SECTOR,
                    fieldMapping = ImportFieldMapping(
                        longitudeHeader = "lng",
                        latitudeHeader = "lat",
                        azimuthHeader = "bearing",
                        nameHeader = "\u7ad9\u70b9\u540d",
                        siteIdHeader = "site_no",
                        pciHeader = "pci_no",
                    ),
                ),
            ),
        )

        assertEquals(1, result.records.size)
        val record = result.records.single()
        assertEquals("\u5019\u9009\u6247\u533aA", record.siteName)
        assertEquals("SITE-2", record.siteId)
        assertEquals("123", record.pci)
        assertEquals(45.0, record.azimuth)
        assertFalse(record.isPointOnly)
    }

    @Test
    fun `parse sheets only keeps selected detail headers in raw fields`() {
        val importer = EngineeringParamImporter()

        val result = importer.parseSheets(
            sheets = listOf(
                ParsedSheet(
                    name = "LTE",
                    rows = listOf(
                        mapOf(
                            "经度" to "120.75686",
                            "纬度" to "34.37036",
                            "小区名称" to "测试小区A",
                            "站高" to "35",
                            "备注" to "楼顶站",
                            "厂家" to "中兴",
                        ),
                    ),
                ),
            ),
            layerId = "layer-detail",
            options = ImportOptions(
                sheetConfig = ImportSheetConfig(
                    selectedSheetName = "LTE",
                    detailHeaders = listOf("站高", "备注"),
                ),
            ),
        )

        val record = result.records.single()
        assertEquals(mapOf("站高" to "35", "备注" to "楼顶站"), record.rawFields)
        assertNull(record.rawFields["厂家"])
    }

}
