package com.xywy.signalmap.domain.map

import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord

object MapRecordFilter {
    fun matches(
        record: SectorRecord,
        technologies: Set<RadioTechnology>,
        bands: Set<String>,
    ): Boolean {
        // Generic Excel, KML and manually drawn features are not radio-network filters.
        if (record.isPointOnly || record.radioTechnology == RadioTechnology.UNKNOWN ||
            record.polygonCoordinates.isNotEmpty()
        ) return true

        return (technologies.isEmpty() || record.radioTechnology in technologies) &&
            (bands.isEmpty() || record.band.orEmpty() in bands)
    }
}
