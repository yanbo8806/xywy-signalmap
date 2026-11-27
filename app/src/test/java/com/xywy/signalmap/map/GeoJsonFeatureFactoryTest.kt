package com.xywy.signalmap.map

import com.xywy.signalmap.domain.geo.SectorGeometry
import com.xywy.signalmap.domain.geo.SectorStyleCatalog
import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.LayerPointIconPreset
import com.xywy.signalmap.domain.model.LocationFix
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import com.xywy.signalmap.domain.model.SimCellMatch
import com.xywy.signalmap.domain.model.TestLogCellRecord
import com.xywy.signalmap.domain.model.TestLogPoint
import com.xywy.signalmap.domain.model.TestLogSample
import com.xywy.signalmap.domain.model.TestLogSimRecord
import com.xywy.signalmap.domain.model.TestMetricMode
import org.maplibre.geojson.LineString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoJsonFeatureFactoryTest {
    @Test
    fun `site features and labels are deduplicated by site and prefer nr style`() {
        val records = listOf(
            SectorRecord(
                id = "nr-1",
                layerId = "layer",
                radioTechnology = RadioTechnology.NR,
                siteId = "site-a",
                siteName = "站点A",
                longitude = 120.0,
                latitude = 34.0,
            ),
            SectorRecord(
                id = "lte-1",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                siteId = "site-a",
                siteName = "站点A",
                longitude = 120.0,
                latitude = 34.0,
            ),
            SectorRecord(
                id = "lte-2",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                siteId = "site-b",
                siteName = "站点B",
                longitude = 120.1,
                latitude = 34.1,
            ),
        )

        val siteFeatures = GeoJsonFeatureFactory.siteFeatures(records)
        val labelFeatures = GeoJsonFeatureFactory.siteLabelFeatures(records)

        assertEquals(2, siteFeatures.features()!!.size)
        assertEquals(2, labelFeatures.features()!!.size)

        val siteALabel = labelFeatures.features()!!.first { it.getStringProperty("title") == "站点A" }
        val siteAFeature = siteFeatures.features()!!.first { it.getStringProperty("title") == "站点A" }
        assertEquals("#FFFFFF", siteALabel.getStringProperty("labelColor"))
        assertEquals("#111111", siteALabel.getStringProperty("labelHaloColor"))
        assertTrue(siteALabel.getStringProperty("labelId").startsWith("site-label-layer-nr-1-"))
        assertEquals("nr-1", siteALabel.getStringProperty("recordId"))
        assertEquals("nr-1", siteAFeature.getStringProperty("recordId"))

        val siteBLabel = labelFeatures.features()!!.first { it.getStringProperty("title") == "站点B" }
        assertEquals("#9AD4FF", siteBLabel.getStringProperty("labelColor"))
        assertEquals("#D4AF37", siteBLabel.getStringProperty("labelHaloColor"))
        assertTrue(siteBLabel.getStringProperty("labelId").startsWith("site-label-layer-lte-2-"))
        assertEquals("lte-2", siteBLabel.getStringProperty("recordId"))
    }

    @Test
    fun `sector features skip point only records but site features still include them`() {
        val records = listOf(
            SectorRecord(
                id = "sector-1",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                siteName = "小区A",
                longitude = 120.0,
                latitude = 34.0,
            ),
            SectorRecord(
                id = "point-1",
                layerId = "layer",
                radioTechnology = RadioTechnology.UNKNOWN,
                siteName = "点位A",
                longitude = 120.2,
                latitude = 34.2,
                isPointOnly = true,
            ),
        )

        val sectorFeatures = GeoJsonFeatureFactory.sectorFeatures(records)
        val siteFeatures = GeoJsonFeatureFactory.siteFeatures(records)

        assertEquals(1, sectorFeatures.features()!!.size)
        assertEquals(2, siteFeatures.features()!!.size)
    }

    @Test
    fun `sector features render user line records as linestring`() {
        val records = listOf(
            SectorRecord(
                id = "line-1",
                layerId = "layer",
                radioTechnology = RadioTechnology.UNKNOWN,
                siteName = "线路A",
                longitude = 120.0,
                latitude = 34.0,
                polygonCoordinates = listOf(
                    com.xywy.signalmap.domain.model.GeoPoint(120.0, 34.0),
                    com.xywy.signalmap.domain.model.GeoPoint(120.1, 34.1),
                ),
                rawFields = mapOf("类型" to "线", "type" to "line"),
            ),
        )

        val features = GeoJsonFeatureFactory.sectorFeatures(records).features().orEmpty()

        assertEquals(1, features.size)
        assertTrue(features.single().geometry() is LineString)
    }

    @Test
    fun `site label features respect per layer label visibility and colors`() {
        val layer = LayerMetadata(
            id = "layer",
            name = "测试图层",
            sourceFileName = "demo.xlsx",
            importedAtMillis = 1L,
            isVisible = true,
            recordCount = 1,
            showLabels = true,
            labelTextColor = "#00FFAA",
            labelHaloColor = "#221144",
        )
        val hiddenLayer = layer.copy(
            id = "hidden",
            showLabels = false,
        )
        val records = listOf(
            SectorRecord(
                id = "sector-1",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                siteName = "站点A",
                longitude = 120.0,
                latitude = 34.0,
            ),
            SectorRecord(
                id = "sector-2",
                layerId = "hidden",
                radioTechnology = RadioTechnology.NR,
                siteName = "站点B",
                longitude = 120.1,
                latitude = 34.1,
            ),
        )

        val labels = GeoJsonFeatureFactory.siteLabelFeatures(
            records = records,
            layersById = mapOf(layer.id to layer, hiddenLayer.id to hiddenLayer),
        )

        assertEquals(1, labels.features()!!.size)
        val feature = labels.features()!!.single()
        assertEquals("站点A", feature.getStringProperty("title"))
        assertEquals("#00FFAA", feature.getStringProperty("labelColor"))
        assertEquals("#221144", feature.getStringProperty("labelHaloColor"))
    }

    @Test
    fun `site label image key changes when title changes`() {
        val records = listOf(
            SectorRecord(
                id = "point-1",
                layerId = "layer",
                radioTechnology = RadioTechnology.UNKNOWN,
                siteId = "fixed-site-id",
                siteName = "旧名称",
                longitude = 120.0,
                latitude = 34.0,
                isPointOnly = true,
            ),
        )
        val updatedRecords = listOf(
            records.single().copy(
                siteName = "新名称",
                cellName = "新名称",
            ),
        )

        val firstLabelId = GeoJsonFeatureFactory.siteLabelFeatures(records).features()!!.single().getStringProperty("labelId")
        val updatedLabelId = GeoJsonFeatureFactory.siteLabelFeatures(updatedRecords).features()!!.single().getStringProperty("labelId")

        assertEquals(false, firstLabelId == updatedLabelId)
    }

    @Test
    fun `point icon features expose preset icon id from layer style`() {
        val layer = LayerMetadata(
            id = "layer",
            name = "点图层",
            sourceFileName = "points.xlsx",
            importedAtMillis = 1L,
            isVisible = true,
            recordCount = 1,
            pointIconPreset = LayerPointIconPreset.BLUE_PIN,
        )
        val records = listOf(
            SectorRecord(
                id = "point-1",
                layerId = "layer",
                radioTechnology = RadioTechnology.UNKNOWN,
                siteName = "点位A",
                longitude = 120.2,
                latitude = 34.2,
                isPointOnly = true,
            ),
        )

        val icons = GeoJsonFeatureFactory.sitePointIconFeatures(
            records = records,
            layersById = mapOf(layer.id to layer),
        )
        val iconFeatures = icons.features().orEmpty()

        assertEquals(1, iconFeatures.size)
        assertEquals("point-icon-blue-pin", iconFeatures.single().getStringProperty("iconId"))
    }

    @Test
    fun `point icon features prefer per record icon preset when present`() {
        val layer = LayerMetadata(
            id = "layer",
            name = "点图层",
            sourceFileName = "points.xlsx",
            importedAtMillis = 1L,
            isVisible = true,
            recordCount = 1,
            pointIconPreset = LayerPointIconPreset.BLUE_PIN,
        )
        val records = listOf(
            SectorRecord(
                id = "point-1",
                layerId = "layer",
                radioTechnology = RadioTechnology.UNKNOWN,
                siteName = "点位A",
                longitude = 120.2,
                latitude = 34.2,
                isPointOnly = true,
                rawFields = mapOf("icon" to LayerPointIconPreset.PURPLE_PIN.name),
            ),
        )

        val icons = GeoJsonFeatureFactory.sitePointIconFeatures(
            records = records,
            layersById = mapOf(layer.id to layer),
        ).features().orEmpty()

        assertEquals(1, icons.size)
        assertEquals("point-icon-purple-pin", icons.single().getStringProperty("iconId"))
    }

    @Test
    fun `point icon features also include engineering site points for sector layers`() {
        val layer = LayerMetadata(
            id = "layer",
            name = "工参图层",
            sourceFileName = "sites.xlsx",
            importedAtMillis = 1L,
            isVisible = true,
            recordCount = 1,
            pointIconPreset = LayerPointIconPreset.GREEN_PIN,
        )
        val records = listOf(
            SectorRecord(
                id = "sector-1",
                layerId = "layer",
                radioTechnology = RadioTechnology.LTE,
                siteId = "site-a",
                siteName = "基站A",
                cellName = "小区A1",
                longitude = 120.2,
                latitude = 34.2,
            ),
        )

        val icons = GeoJsonFeatureFactory.sitePointIconFeatures(
            records = records,
            layersById = mapOf(layer.id to layer),
        )
        val iconFeatures = icons.features().orEmpty()

        assertEquals(1, iconFeatures.size)
        assertEquals("基站A", iconFeatures.single().getStringProperty("title"))
        assertEquals("point-icon-green-pin", iconFeatures.single().getStringProperty("iconId"))
    }

    @Test
    fun `serving line features can be filtered by slot visibility`() {
        val location = LocationFix(
            longitude = 120.9,
            latitude = 34.3,
            accuracyMeters = 5f,
            timeMillis = 1L,
        )
        val simMatches = listOf(
            SimCellMatch(
                subscriptionId = 1,
                slotIndex = 0,
                label = "SIM1",
                servingCell = CellSignal(radioTechnology = RadioTechnology.LTE, registered = true, cellId = "101"),
                servingSector = SectorRecord(
                    id = "lte-1",
                    layerId = "layer",
                    radioTechnology = RadioTechnology.LTE,
                    siteName = "站点A",
                    longitude = 120.91,
                    latitude = 34.31,
                ),
            ),
            SimCellMatch(
                subscriptionId = 2,
                slotIndex = 1,
                label = "SIM2",
                servingCell = CellSignal(radioTechnology = RadioTechnology.NR, registered = true, cellId = "201"),
                servingSector = SectorRecord(
                    id = "nr-1",
                    layerId = "layer",
                    radioTechnology = RadioTechnology.NR,
                    siteName = "站点B",
                    longitude = 120.92,
                    latitude = 34.32,
                ),
            ),
        )

        val features = GeoJsonFeatureFactory.servingLineFeatures(
            locationFix = location,
            simMatches = simMatches,
            visibleSlotIndexes = setOf(1),
        )

        assertEquals(1, features.features()!!.size)
        assertEquals("SIM2", features.features()!!.single().getStringProperty("simLabel"))
    }

    @Test
    fun `serving line targets rendered sector triangle base midpoint`() {
        val location = LocationFix(
            longitude = 120.9,
            latitude = 34.3,
            accuracyMeters = 5f,
            timeMillis = 1L,
        )
        val sector = SectorRecord(
            id = "lte-sector",
            layerId = "layer",
            radioTechnology = RadioTechnology.LTE,
            longitude = 120.91,
            latitude = 34.31,
            azimuth = 90.0,
            band = "B3",
        )
        val halfBeam = SectorStyleCatalog.beamWidthDegrees(sector) / 2.0
        val radiusMeters = SectorStyleCatalog.radiusMeters(sector)
        val left = SectorGeometry.destinationPoint(
            longitude = sector.longitude,
            latitude = sector.latitude,
            bearingDegrees = sector.azimuth!! - halfBeam,
            distanceMeters = radiusMeters,
        )
        val right = SectorGeometry.destinationPoint(
            longitude = sector.longitude,
            latitude = sector.latitude,
            bearingDegrees = sector.azimuth + halfBeam,
            distanceMeters = radiusMeters,
        )
        val expectedLongitude = (left.first + right.first) / 2.0
        val expectedLatitude = (left.second + right.second) / 2.0

        val features = GeoJsonFeatureFactory.servingLineFeatures(
            locationFix = location,
            simMatches = listOf(
                SimCellMatch(
                    subscriptionId = 1,
                    slotIndex = 0,
                    label = "SIM1",
                    servingSector = sector,
                ),
            ),
        )

        val target = ((features.features()!!.single().geometry() as LineString).coordinates()).last()
        assertClose(expectedLongitude, target.longitude())
        assertClose(expectedLatitude, target.latitude())
        assertTrue(kotlin.math.abs(target.longitude() - sector.longitude) > 0.00001)
    }


    @Test
    fun `test log points can use visible sim slot metrics`() {
        val sample = TestLogSample(
            timestampMillis = 1000L,
            point = TestLogPoint(
                longitude = 120.9,
                latitude = 34.3,
            ),
            sims = listOf(
                TestLogSimRecord(
                    slotIndex = 0,
                    label = "SIM1",
                    servingCell = TestLogCellRecord(
                        radioTechnology = RadioTechnology.LTE,
                        rsrp = -95,
                        sinr = 8,
                    ),
                ),
                TestLogSimRecord(
                    slotIndex = 1,
                    label = "SIM2",
                    servingCell = TestLogCellRecord(
                        radioTechnology = RadioTechnology.NR,
                        rsrp = -112,
                        sinr = -2,
                    ),
                ),
            ),
        )

        val features = GeoJsonFeatureFactory.testLogPointFeatures(
            samples = listOf(sample),
            metricMode = TestMetricMode.RSRP,
            visibleSlotIndexes = setOf(1),
        )

        assertEquals(1, features.features()!!.size)
        val feature = features.features()!!.single()
        assertEquals("SIM2", feature.getStringProperty("metricSource"))
        assertEquals(-112, feature.getNumberProperty("metricValue").toInt())
    }

    @Test
    fun `test log points emit one feature per visible sim and expose slot index`() {
        val sample = TestLogSample(
            timestampMillis = 1000L,
            point = TestLogPoint(
                longitude = 120.9,
                latitude = 34.3,
            ),
            sims = listOf(
                TestLogSimRecord(
                    slotIndex = 0,
                    label = "SIM1",
                    servingCell = TestLogCellRecord(
                        radioTechnology = RadioTechnology.LTE,
                        rsrp = -95,
                        sinr = 8,
                    ),
                ),
                TestLogSimRecord(
                    slotIndex = 1,
                    label = "SIM2",
                    servingCell = TestLogCellRecord(
                        radioTechnology = RadioTechnology.NR,
                        rsrp = -112,
                        sinr = -2,
                    ),
                ),
            ),
        )

        val features = GeoJsonFeatureFactory.testLogPointFeatures(
            samples = listOf(sample),
            metricMode = TestMetricMode.RSRP,
            visibleSlotIndexes = setOf(0, 1),
        ).features().orEmpty()

        assertEquals(2, features.size)
        assertEquals(listOf(0, 1), features.map { it.getNumberProperty("slotIndex").toInt() }.sorted())
        assertEquals(listOf("SIM1", "SIM2"), features.map { it.getStringProperty("metricSource") }.sorted())
    }

    @Test
    fun `user polygon draft features expose fill line point and label geometry`() {
        val features = GeoJsonFeatureFactory.userPolygonDraftFeatures(
            points = listOf(
                com.xywy.signalmap.domain.model.GeoPoint(120.0, 34.0),
                com.xywy.signalmap.domain.model.GeoPoint(120.1, 34.0),
                com.xywy.signalmap.domain.model.GeoPoint(120.1, 34.1),
            ),
            name = "面A",
        )

        assertEquals(1, features.fillFeatures.features().orEmpty().size)
        assertEquals(1, features.lineFeatures.features().orEmpty().size)
        assertEquals(3, features.pointFeatures.features().orEmpty().size)
        assertEquals("面A · 3点", features.labelFeatures.features().orEmpty().single().getStringProperty("title"))
    }

    private fun assertClose(
        expected: Double,
        actual: Double,
        tolerance: Double = 0.000001,
    ) {
        assertTrue(
            kotlin.math.abs(expected - actual) <= tolerance,
            "Expected <$expected> but was <$actual>",
        )
    }
}
