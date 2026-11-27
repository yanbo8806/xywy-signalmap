package com.xywy.signalmap.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class RadioTechnology {
    NR,
    LTE,
    WCDMA,
    GSM,
    CDMA,
    UNKNOWN;

    val label: String
        get() = when (this) {
            NR -> "NR"
            LTE -> "LTE"
            WCDMA -> "WCDMA"
            GSM -> "GSM"
            CDMA -> "CDMA"
            UNKNOWN -> "未知"
        }

    companion object {
        fun fromRaw(value: String?): RadioTechnology {
            val normalized = value.orEmpty().trim().uppercase()
            return when {
                normalized.contains("NR") || normalized.contains("5G") -> NR
                normalized.contains("LTE") || normalized.contains("4G") -> LTE
                normalized.contains("WCDMA") || normalized.contains("UMTS") || normalized == "3G" -> WCDMA
                normalized.contains("GSM") || normalized.contains("2G") -> GSM
                normalized.contains("CDMA") || normalized.contains("1X") || normalized.contains("EVDO") -> CDMA
                else -> UNKNOWN
            }
        }
    }
}
