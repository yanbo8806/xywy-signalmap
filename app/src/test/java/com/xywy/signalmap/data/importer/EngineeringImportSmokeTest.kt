package com.xywy.signalmap.data.importer

import com.xywy.signalmap.domain.cell.SimCellMatcher
import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.LocationFix
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SimCellSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals

class EngineeringImportSmokeTest {
    @Test
    fun syntheticEngineeringRowImportsAndMatchesTheServingCell() {
        val row = linkedMapOf(
            "\u7F51\u5143\u540D\u79F0" to "Demo Tower",
            "\u57FA\u7AD9\u7F16\u53F7" to "DEMO-NR-01",
            "gNODEB ID" to "NODE-DEMO-01",
            "CGI(\u5C0F\u533A\u552F\u4E00\u6807\u8BC6)" to "001-01-000020-20",
            "ECI\uFF08\u552F\u4E00\u6807\u8BC6\u5217\uFF09" to "TEST-ECI-01",
            "\u5C0F\u533A\u540D\u79F0" to "Demo Cell",
            "\u7ECF\u5EA6" to "120.4567",
            "\u7EAC\u5EA6" to "34.3456",
            "PCI" to "401",
            "\u9891\u5E26\u6307\u793A" to "n41",
        )
        val parsed = EngineeringParamImporter().parseSheets(
            sheets = listOf(ParsedSheet("NR", listOf(row))),
            layerId = "synthetic-fixture",
        )
        val sector = parsed.records.single()
        val matches = SimCellMatcher.match(
            simSnapshots = listOf(
                SimCellSnapshot(
                    subscriptionId = 1,
                    slotIndex = 0,
                    label = "SIM1",
                    servingCell = CellSignal(
                        radioTechnology = RadioTechnology.NR,
                        registered = true,
                        cellId = "TEST-ECI-01",
                        pci = "401",
                        band = "N41",
                    ),
                ),
            ),
            sectors = parsed.records,
            currentLocation = LocationFix(sector.longitude, sector.latitude, 5f, 1L),
        )

        assertEquals("TEST-ECI-01", sector.cellId)
        assertEquals("Demo Cell", matches.single().servingSector?.cellName)
    }
}
