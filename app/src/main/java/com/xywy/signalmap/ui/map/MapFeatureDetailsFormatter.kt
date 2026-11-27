package com.xywy.signalmap.ui.map

import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.SectorRecord
import java.util.Locale

data class MapFeatureDetailRow(
    val label: String,
    val value: String,
)

object MapFeatureDetailsFormatter {
    fun build(record: SectorRecord, layer: LayerMetadata): List<MapFeatureDetailRow> {
        val rows = mutableListOf<MapFeatureDetailRow>()
        val usedLabels = linkedSetOf<String>()

        fun append(label: String, value: String?) {
            val resolved = value?.trim()?.takeIf { it.isNotBlank() } ?: return
            if (usedLabels.add(label)) {
                rows += MapFeatureDetailRow(label = label, value = resolved)
            }
        }

        if (layer.userEditable && record.isPointOnly) {
            append("名称", record.cellName ?: record.siteName ?: record.rawFields["名称"] ?: record.id)
            append("经度", formatCoordinate(record.longitude))
            append("纬度", formatCoordinate(record.latitude))
            append("备注", record.rawFields["备注"] ?: record.rawFields["remark"] ?: record.rawFields["note"])
            return rows
        }

        append("\u56FE\u5C42", layer.name)
        append("\u7C7B\u578B", if (record.isPointOnly) "\u70B9\u4F4D" else "\u6247\u533A")
        append("\u5236\u5F0F", record.radioTechnology.label.takeIf { record.radioTechnology != RadioTechnology.UNKNOWN })
        append("\u7AD9\u540D", record.siteName)
        append("\u5C0F\u533A\u540D", record.cellName)
        append("\u57FA\u7AD9ID", record.siteId)
        append("\u5C0F\u533AID", record.cellId)
        append("PCI", record.pci)
        append("TAC/LAC", record.tacOrLac)
        append("Band", record.band)
        append("\u5E26\u5BBD", record.bandwidth)
        append("\u7ECF\u5EA6", formatCoordinate(record.longitude))
        append("\u7EAC\u5EA6", formatCoordinate(record.latitude))
        append("\u65B9\u4F4D\u89D2", record.azimuth?.let(::formatAzimuth))
        append("\u7AD9\u9AD8", record.antennaHeight)

        layer.detailFieldHeaders.forEach { header ->
            append(header, record.rawFields[header])
        }
        return rows
    }

    private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

    private fun formatAzimuth(value: Double): String = String.format(Locale.US, "%.1f\u00B0", value)
}
