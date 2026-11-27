package com.xywy.signalmap.map

import com.xywy.signalmap.data.importer.EngineeringParamImporter
import com.xywy.signalmap.data.importer.ImportOptions
import com.xywy.signalmap.data.importer.ImportSheetConfig
import com.xywy.signalmap.data.importer.ParsedSheet
import com.xywy.signalmap.domain.map.MapImportFocusPlanner
import com.xywy.signalmap.domain.map.MapOverlayRenderPolicy
import com.xywy.signalmap.domain.map.MapRecordFilter
import com.xywy.signalmap.domain.model.ImportedLayerRenderMode
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.LayerPointIconPreset
import com.xywy.signalmap.domain.model.MapControlPreferences
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ExcelLayerRenderingRegressionTest {
    @Test
    fun `name coordinates and azimuth render in every import mode with default radio filters`() {
        val engineering = (0 until 2000).map {
            SectorRecord("old-$it", "engineering", RadioTechnology.LTE, longitude = 120.8 + it * .000001, latitude = 34.4)
        }
        val controls = MapControlPreferences()
        listOf(ImportedLayerRenderMode.AUTO, ImportedLayerRenderMode.POINT, ImportedLayerRenderMode.SECTOR).forEach { mode ->
            val imported = EngineeringParamImporter().parseSheets(
                sheets = listOf(ParsedSheet("Sheet1", listOf(mapOf(
                    "名称" to "普通表格点位", "经度" to "120.81", "纬度" to "34.41", "方位角" to "60",
                )))),
                layerId = "excel",
                options = ImportOptions(sheetConfig = ImportSheetConfig("Sheet1", renderMode = mode)),
            )
            assertEquals(1, imported.records.size)
            val visible = (engineering + imported.records).filter {
                MapRecordFilter.matches(it, controls.selectedTechs, controls.selectedBands)
            }
            assertTrue(visible.any { it.layerId == "excel" })
            val focus = assertNotNull(MapImportFocusPlanner.resolve(imported.records))
            val policy = MapOverlayRenderPolicy.resolve(focus.zoom, true, true, true, true, true)
            val payload = MapVectorRenderPayloadFactory.createFullLayer(
                displayRecords = visible,
                layersById = mapOf("excel" to LayerMetadata("excel", "Excel", "example.xlsx", 0, true, 1)),
                renderPolicy = policy,
            )
            val recordId = imported.records.single().id
            assertTrue(payload.pointIconFeatures.features().orEmpty().any { it.getStringProperty("recordId") == recordId }, "$mode point must be visible")
            assertTrue(payload.siteLabelFeatures.features().orEmpty().any { it.getStringProperty("recordId") == recordId }, "$mode label must be visible")
            if (mode != ImportedLayerRenderMode.POINT) {
                assertTrue(payload.sectorFeatures.features().orEmpty().any { it.getStringProperty("recordId") == recordId }, "$mode sector must be visible")
            }
        }
    }

    @Test
    fun `point overview remains populated at the old import zoom`() {
        val record = SectorRecord("point", "excel", RadioTechnology.UNKNOWN, longitude = 120.8, latitude = 34.4, isPointOnly = true)
        val policy = MapOverlayRenderPolicy.resolve(8.0, true, true, true, true, true)
        val payload = MapVectorRenderPayloadFactory.createFullLayer(listOf(record), emptyMap(), policy)
        assertEquals(1, payload.pointIconFeatures.features().orEmpty().size)
        assertEquals(0, payload.sectorFeatures.features().orEmpty().size)
    }

    @Test
    fun `point mode survives a persisted engineering site switch and preserves icon styles`() {
        // This reproduces the reported label-only state before separating the two controls.
        val imported = EngineeringParamImporter().parseSheets(
            listOf(ParsedSheet("Sheet1", listOf(mapOf(
                "名称" to "独立点位", "经度" to "120.81", "纬度" to "34.41", "方位角" to "60",
            )))),
            "points", ImportOptions(sheetConfig = ImportSheetConfig("Sheet1", ImportedLayerRenderMode.POINT)),
        ).records
        val sector = SectorRecord("engineering", "engineering", RadioTechnology.LTE, longitude = 120.8, latitude = 34.4)
        val policy = MapOverlayRenderPolicy.resolve(
            15.0, true, false, true, true, true, wantPointIcons = true,
        )
        val layer = LayerMetadata(
            "points", "图标点", "points.xlsx", 0, true, 1, renderMode = ImportedLayerRenderMode.POINT,
        )
        var previousSignature: MapVectorRenderSignature? = null
        LayerPointIconPreset.entries.forEach { preset ->
            val style = layer.copy(pointIconPreset = preset, labelTextColor = "#00FF00", labelHaloColor = "#111111")
            val records = listOf(sector) + imported
            val payload = MapVectorRenderPayloadFactory.create(records, records, imported, mapOf(layer.id to style), policy)
            assertEquals(0, payload.circleFeatures.features().orEmpty().size)
            val icon = payload.pointIconFeatures.features().orEmpty().single()
            assertEquals(imported.single().id, icon.getStringProperty("recordId"))
            assertEquals(preset.iconId, icon.getStringProperty("iconId"))
            assertEquals("#00FF00", payload.siteLabelFeatures.features().orEmpty().single().getStringProperty("labelColor"))
            assertNotEquals(previousSignature, payload.signature)
            previousSignature = payload.signature
        }
        val labelsOff = MapVectorRenderPayloadFactory.create(
            imported, imported, imported, mapOf(layer.id to layer.copy(showLabels = false)), policy,
        )
        assertEquals(1, labelsOff.pointIconFeatures.features().orEmpty().size)
        assertEquals(0, labelsOff.siteLabelFeatures.features().orEmpty().size)
        val layerHidden = MapVectorRenderPayloadFactory.create(
            imported, imported, imported, mapOf(layer.id to layer.copy(isVisible = false)), policy,
        )
        assertEquals(0, layerHidden.pointIconFeatures.features().orEmpty().size)
        assertEquals(0, layerHidden.siteLabelFeatures.features().orEmpty().size)
    }
}
