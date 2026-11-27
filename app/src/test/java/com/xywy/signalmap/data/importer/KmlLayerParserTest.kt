package com.xywy.signalmap.data.importer

import com.xywy.signalmap.domain.model.RadioTechnology
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import org.xml.sax.SAXException

class KmlLayerParserTest {
    @Test
    fun `prefixed kml elements retain names coordinates and extended fields`() {
        val xml = """
            <k:kml xmlns:k="http://www.opengis.net/kml/2.2"><k:Placemark>
              <k:name>前缀点</k:name>
              <k:ExtendedData><k:Data name="频带指示"><k:value>N41</k:value></k:Data></k:ExtendedData>
              <k:Point><k:coordinates>120.8,34.4,0</k:coordinates></k:Point>
            </k:Placemark></k:kml>
        """.trimIndent()
        val record = KmlLayerParser().parse(xml.byteInputStream(), "prefix").records.single()
        assertEquals("前缀点", record.siteName)
        assertEquals(120.8, record.longitude)
        assertEquals("N41", record.band)
        assertTrue(record.isPointOnly)
    }

    @Test
    fun `invalid coordinates and degenerate geometries are skipped while valid points survive`() {
        val invalid = listOf("NaN,34", "120,Infinity", "181,34", "120,-91")
            .joinToString("") { "<Placemark><Point><coordinates>$it</coordinates></Point></Placemark>" }
        val xml = """
            <kml>$invalid
              <Placemark><LineString><coordinates>120,30</coordinates></LineString></Placemark>
              <Placemark><Polygon><coordinates>120,30 120,30</coordinates></Polygon></Placemark>
              <Placemark><Point><coordinates>120.8,34.4</coordinates></Point></Placemark>
            </kml>
        """.trimIndent()
        val result = KmlLayerParser().parse(xml.byteInputStream(), "invalid")
        assertEquals(6, result.skippedCount)
        assertEquals(120.8, result.records.single().longitude)
        assertTrue(result.records.single().latitude.isFinite())
    }

    @Test
    fun `kml rejects external entity declarations`() {
        val xml = """
            <!DOCTYPE kml [<!ENTITY external SYSTEM "file:///nonexistent-private-file">]>
            <kml><Placemark><name>&external;</name>
            <Point><coordinates>120,30</coordinates></Point></Placemark></kml>
        """.trimIndent()
        assertFailsWith<SAXException> { KmlLayerParser().parse(xml.byteInputStream(), "external") }
    }

    @Test
    fun `kml parser reads polygon placemark`() {
        val parser = KmlLayerParser()
        val kml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
              <Document>
                <Placemark>
                  <name>5G-N41-测试小区</name>
                  <ExtendedData>
                    <Data name="频带指示"><value>n41</value></Data>
                    <Data name="方位角（度）"><value>60</value></Data>
                  </ExtendedData>
                  <Polygon>
                    <outerBoundaryIs>
                      <LinearRing>
                        <coordinates>
                          120.0000,34.0000,0
                          120.0100,34.0000,0
                          120.0100,34.0100,0
                          120.0000,34.0000,0
                        </coordinates>
                      </LinearRing>
                    </outerBoundaryIs>
                  </Polygon>
                </Placemark>
              </Document>
            </kml>
        """.trimIndent()

        val result = parser.parse(
            input = kml.byteInputStream(),
            layerId = "layer-1",
        )

        assertEquals(1, result.records.size)
        val record = result.records.single()
        assertEquals(RadioTechnology.NR, record.radioTechnology)
        assertEquals("N41", record.band)
        assertTrue(record.polygonCoordinates.isNotEmpty())
        assertEquals(4, record.polygonCoordinates.size)
    }


    @Test
    fun `kml parser marks point placemark as point only`() {
        val parser = KmlLayerParser()
        val kml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
              <Document>
                <Placemark>
                  <name>监测点1</name>
                  <Point>
                    <coordinates>120.1234,34.5678,0</coordinates>
                  </Point>
                </Placemark>
              </Document>
            </kml>
        """.trimIndent()

        val result = parser.parse(
            input = kml.byteInputStream(),
            layerId = "layer-point",
        )

        assertEquals(1, result.records.size)
        val record = result.records.single()
        assertEquals("监测点1", record.siteName)
        assertTrue(record.isPointOnly)
        assertTrue(record.polygonCoordinates.isEmpty())
    }

    @Test
    fun `kml parser reads linestring placemark as editable line`() {
        val parser = KmlLayerParser()
        val kml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
              <Document>
                <Placemark>
                  <name>巡检线1</name>
                  <LineString>
                    <coordinates>
                      120.1000,34.1000,0
                      120.1100,34.1100,0
                    </coordinates>
                  </LineString>
                </Placemark>
              </Document>
            </kml>
        """.trimIndent()

        val result = parser.parse(
            input = kml.byteInputStream(),
            layerId = "layer-line",
        )

        assertEquals(1, result.records.size)
        val record = result.records.single()
        assertEquals("巡检线1", record.siteName)
        assertTrue(record.isLineOnly)
        assertEquals(2, record.polygonCoordinates.size)
    }

}
