package com.xywy.signalmap.data.exporter

import com.xywy.signalmap.domain.model.GeoPoint
import com.xywy.signalmap.domain.model.ImportedLayerRenderMode
import com.xywy.signalmap.domain.model.LayerDocument
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import kotlin.test.Test
import kotlin.test.assertContains

class UserLayerExportCodecTest {
    @Test
    fun `exports user points lines and polygons as csv`() {
        val csv = UserLayerExportCodec.toCsv(sampleDocument())

        assertContains(csv, "name,type,longitude,latitude,icon,polygon")
        assertContains(csv, "\"点A\",\"point\",120.80000000,34.40000000")
        assertContains(csv, "\"线A\",\"line\",120.80500000,34.40500000")
        assertContains(csv, "\"面A\",\"polygon\",120.81000000,34.41000000")
        assertContains(csv, "120.81000000 34.41000000;120.82000000 34.41000000")
        assertContains(csv, "\"备注A\"")
    }

    @Test
    fun `exports user points lines and polygons as kml`() {
        val kml = UserLayerExportCodec.toKml(sampleDocument())

        assertContains(kml, "<name>手动图层</name>")
        assertContains(kml, "<Point><coordinates>120.80000000,34.40000000,0</coordinates></Point>")
        assertContains(kml, "<LineString><coordinates>")
        assertContains(kml, "120.80500000,34.40500000,0 120.80600000,34.40600000,0")
        assertContains(kml, "<Polygon><outerBoundaryIs><LinearRing><coordinates>")
        assertContains(kml, "120.81000000,34.41000000,0")
    }

    private fun sampleDocument() = LayerDocument(
        meta = LayerMetadata(
            id = "layer",
            name = "手动图层",
            sourceFileName = "手动新建",
            importedAtMillis = 1L,
            isVisible = true,
            recordCount = 3,
            renderMode = ImportedLayerRenderMode.USER,
            userEditable = true,
        ),
        sectors = listOf(
            SectorRecord(
                id = "p1",
                layerId = "layer",
                radioTechnology = RadioTechnology.UNKNOWN,
                siteName = "点A",
                cellName = "点A",
                longitude = 120.8,
                latitude = 34.4,
                isPointOnly = true,
                rawFields = mapOf("icon" to "RED_PIN", "备注" to "备注A"),
            ),
            SectorRecord(
                id = "l1",
                layerId = "layer",
                radioTechnology = RadioTechnology.UNKNOWN,
                siteName = "线A",
                cellName = "线A",
                longitude = 120.805,
                latitude = 34.405,
                polygonCoordinates = listOf(
                    GeoPoint(120.805, 34.405),
                    GeoPoint(120.806, 34.406),
                ),
                rawFields = mapOf("类型" to "线", "type" to "line"),
            ),
            SectorRecord(
                id = "g1",
                layerId = "layer",
                radioTechnology = RadioTechnology.UNKNOWN,
                siteName = "面A",
                cellName = "面A",
                longitude = 120.81,
                latitude = 34.41,
                polygonCoordinates = listOf(
                    GeoPoint(120.81, 34.41),
                    GeoPoint(120.82, 34.41),
                    GeoPoint(120.82, 34.42),
                ),
                rawFields = mapOf("类型" to "多边形"),
            ),
        ),
    )
}
