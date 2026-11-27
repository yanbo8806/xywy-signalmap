package com.xywy.signalmap.domain.cell

import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.RadioTechnology
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.round

data class CellDisplayDetails(
    val cellIdentityLabel: String,
    val cellIdentityValue: String,
    val baseStationId: String?,
    val localCellId: String?,
    val tacLabel: String,
    val tacValue: String?,
    val channelLabel: String,
    val channelValue: String?,
    val pciLabel: String,
    val pciValue: String?,
    val pciMod3: String?,
    val modeOrNetworkLabel: String,
    val modeOrNetworkValue: String?,
    val frequencyLabel: String?,
    val bandLabel: String?,
    val bandwidthLabel: String?,
    val mcc: String?,
    val mnc: String?,
    val gscn: String?,
)

object CellDisplayFormatter {
    fun from(signal: CellSignal): CellDisplayDetails {
        val identity = parseLong(signal.cellId)
        val channel = parseInt(signal.channelNumber)
        val split = splitIdentity(signal.radioTechnology, identity)
        val normalizedBand = normalizeBand(signal.band)

        return CellDisplayDetails(
            cellIdentityLabel = signal.radioTechnology.cellIdentityLabel(),
            cellIdentityValue = signal.cellId.orEmpty(),
            baseStationId = split?.first,
            localCellId = split?.second,
            tacLabel = signal.radioTechnology.tacLabel(),
            tacValue = signal.tacOrLac?.takeIf(String::isNotBlank),
            channelLabel = signal.radioTechnology.channelLabel(),
            channelValue = signal.channelNumber?.takeIf(String::isNotBlank),
            pciLabel = signal.radioTechnology.pciLabel(),
            pciValue = signal.pci?.takeIf(String::isNotBlank),
            pciMod3 = parseInt(signal.pci)?.let { (it % 3).toString() },
            modeOrNetworkLabel = signal.radioTechnology.modeOrNetworkLabel(),
            modeOrNetworkValue = signal.radioTechnology.modeOrNetworkValue(
                channelNumber = channel,
                band = normalizedBand,
            ),
            frequencyLabel = signal.radioTechnology.toFrequencyLabel(
                channelNumber = channel,
                band = normalizedBand,
            ),
            bandLabel = normalizedBand,
            bandwidthLabel = normalizeBandwidthLabel(signal.bandwidth),
            mcc = signal.mcc?.takeIf(String::isNotBlank),
            mnc = signal.mnc?.takeIf(String::isNotBlank),
            gscn = signal.radioTechnology.toGscn(channel),
        )
    }

    fun resolveBandwidthLabel(
        liveBandwidth: String?,
        sectorBandwidth: String?,
    ): String? = normalizeBandwidthLabel(liveBandwidth) ?: normalizeBandwidthLabel(sectorBandwidth)

    private fun splitIdentity(
        technology: RadioTechnology,
        identity: Long?,
    ): Pair<String, String>? = when {
        identity == null -> null
        technology == RadioTechnology.NR -> {
            val gNodeBId = identity / 4096
            val sectorId = identity % 4096
            gNodeBId.toString() to sectorId.toString()
        }
        technology == RadioTechnology.LTE -> {
            val eNodeBId = identity / 256
            val sectorId = identity % 256
            eNodeBId.toString() to sectorId.toString()
        }
        else -> null
    }

    private fun RadioTechnology.cellIdentityLabel(): String = when (this) {
        RadioTechnology.NR -> "NCI"
        RadioTechnology.LTE -> "ECI"
        RadioTechnology.WCDMA, RadioTechnology.GSM -> "CID"
        RadioTechnology.CDMA -> "BID"
        RadioTechnology.UNKNOWN -> "CellId"
    }

    private fun RadioTechnology.tacLabel(): String = when (this) {
        RadioTechnology.NR, RadioTechnology.LTE -> "TAC"
        RadioTechnology.WCDMA, RadioTechnology.GSM -> "LAC"
        RadioTechnology.CDMA -> "SID"
        RadioTechnology.UNKNOWN -> "TAC/LAC"
    }

    private fun RadioTechnology.channelLabel(): String = when (this) {
        RadioTechnology.NR -> "NRARFCN"
        RadioTechnology.LTE -> "EARFCN"
        RadioTechnology.WCDMA -> "UARFCN"
        RadioTechnology.GSM -> "ARFCN"
        RadioTechnology.CDMA -> "Channel"
        RadioTechnology.UNKNOWN -> "频点"
    }

    private fun RadioTechnology.pciLabel(): String = when (this) {
        RadioTechnology.NR, RadioTechnology.LTE -> "PCI"
        RadioTechnology.WCDMA -> "PSC"
        RadioTechnology.GSM -> "BSIC"
        RadioTechnology.CDMA -> "NID"
        RadioTechnology.UNKNOWN -> "PCI"
    }

    private fun RadioTechnology.modeOrNetworkLabel(): String = when (this) {
        RadioTechnology.LTE -> "模式"
        else -> "网络"
    }

    private fun RadioTechnology.modeOrNetworkValue(
        channelNumber: Int?,
        band: String?,
    ): String? = when (this) {
        RadioTechnology.LTE -> inferLteMode(band = band, earfcn = channelNumber)
        RadioTechnology.UNKNOWN -> null
        else -> label
    }

    private fun RadioTechnology.toFrequencyLabel(
        channelNumber: Int?,
        band: String?,
    ): String? = when (this) {
        RadioTechnology.NR -> channelNumber
            ?.let(::nrArfcnToFrequencyMhz)
            ?.let { NR_GHZ_FORMAT.format(it / 1000.0) + "GHz" }
        RadioTechnology.LTE -> channelNumber
            ?.let { earfcnToFrequencyLabel(it, band) }
        else -> null
    }

    private fun RadioTechnology.toGscn(channelNumber: Int?): String? {
        if (this != RadioTechnology.NR || channelNumber == null) return null
        val frequencyMhz = nrArfcnToFrequencyMhz(channelNumber) ?: return null
        val gscn = nrFrequencyToGscn(frequencyMhz)
        return gscn?.takeIf { it > 0 }?.toString()
    }

    private fun nrArfcnToFrequencyMhz(nrarfcn: Int): Double? {
        val rawFrequency = when (nrarfcn) {
            in 0..599_999 -> nrarfcn * 0.005
            in 600_000..2_016_666 -> 3000.0 + (nrarfcn - 600_000) * 0.015
            in 2_016_667..3_279_165 -> 24250.08 + (nrarfcn - 2_016_667) * 0.06
            else -> return null
        }
        return round(rawFrequency * 10.0) / 10.0
    }

    private fun nrFrequencyToGscn(frequencyMhz: Double): Int? {
        return when {
            frequencyMhz <= 0.0 -> null
            frequencyMhz < 3000.0 -> {
                var matched: Int? = null
                for (offset in intArrayOf(3, 1, 5)) {
                    val candidate = ((frequencyMhz - offset.toDouble() * 0.05) / 1.2).toInt()
                    if (candidate in 0 until 0x9C3) {
                        val gscn = candidate * 3 + (offset - 3) / 2
                        if (gscn in 2 until 0x1D4B) {
                            matched = gscn
                            break
                        }
                    }
                }
                matched
            }
            frequencyMhz < 24250.0 -> 7499 + ((frequencyMhz - 3000.0) / 1.44).toInt()
            frequencyMhz < 100000.0 -> 22256 + ((frequencyMhz - 24250.08) / 17.28).toInt()
            else -> null
        }
    }

    private fun earfcnToFrequencyLabel(
        earfcn: Int,
        band: String?,
    ): String? {
        val spec = LTE_BAND_SPECS[band] ?: return null
        val downlink = spec.dlBaseMHz + (earfcn - spec.dlOffset) * 0.1
        val uplink = if (spec.ulBaseMHz != null && spec.ulOffset != null) {
            val uplinkEarfcn = earfcn + (spec.ulOffset - spec.dlOffset)
            spec.ulBaseMHz + (uplinkEarfcn - spec.ulOffset) * 0.1
        } else {
            null
        }

        val downlinkLabel = formatTrimmedDecimal(downlink)
        val uplinkLabel = uplink
            ?.takeIf { kotlin.math.abs(it - downlink) > 0.0001 }
            ?.let(::formatTrimmedDecimal)

        return uplinkLabel?.let { "$downlinkLabel/$it" } ?: downlinkLabel
    }

    private fun inferLteMode(
        band: String?,
        earfcn: Int?,
    ): String? {
        LTE_BAND_SPECS[band]?.duplexMode?.let { return it }
        if (earfcn == null) return null
        return when (earfcn) {
            in 36200..45589 -> "TDD"
            else -> "FDD"
        }
    }

    private fun normalizeBand(band: String?): String? {
        val normalized = band?.trim()?.uppercase(Locale.US)?.takeIf(String::isNotBlank) ?: return null
        return when {
            normalized.startsWith("B") || normalized.startsWith("N") -> normalized
            normalized.all(Char::isDigit) -> "B$normalized"
            else -> normalized
        }
    }

    private fun parseLong(value: String?): Long? = value
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.toLongOrNull()

    private fun parseInt(value: String?): Int? = value
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.toIntOrNull()

    private fun normalizeBandwidthLabel(value: String?): String? {
        val raw = value?.trim()?.takeIf(String::isNotBlank) ?: return null
        val normalized = raw.uppercase(Locale.US).replace(" ", "")
        val mhzMatch = Regex("""^(\d+(?:\.\d+)?)(M|MHZ)$""").matchEntire(normalized)
        if (mhzMatch != null) {
            return "${mhzMatch.groupValues[1]} MHz"
        }
        val khzMatch = Regex("""^(\d+(?:\.\d+)?)(K|KHZ)$""").matchEntire(normalized)
        if (khzMatch != null) {
            return "${khzMatch.groupValues[1]} kHz"
        }
        return raw
    }

    private fun formatTrimmedDecimal(value: Double): String =
        BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

    private data class LteBandSpec(
        val dlBaseMHz: Double,
        val dlOffset: Int,
        val ulBaseMHz: Double? = null,
        val ulOffset: Int? = null,
        val duplexMode: String,
    )

    private val NR_GHZ_FORMAT = DecimalFormat(
        "0.0",
        DecimalFormatSymbols(Locale.US),
    )

    private val LTE_BAND_SPECS = mapOf(
        "B1" to LteBandSpec(dlBaseMHz = 2110.0, dlOffset = 0, ulBaseMHz = 1920.0, ulOffset = 18000, duplexMode = "FDD"),
        "B3" to LteBandSpec(dlBaseMHz = 1805.0, dlOffset = 1200, ulBaseMHz = 1710.0, ulOffset = 19200, duplexMode = "FDD"),
        "B5" to LteBandSpec(dlBaseMHz = 869.0, dlOffset = 2400, ulBaseMHz = 824.0, ulOffset = 20400, duplexMode = "FDD"),
        "B8" to LteBandSpec(dlBaseMHz = 925.0, dlOffset = 3450, ulBaseMHz = 880.0, ulOffset = 21450, duplexMode = "FDD"),
        "B34" to LteBandSpec(dlBaseMHz = 2010.0, dlOffset = 36200, duplexMode = "TDD"),
        "B38" to LteBandSpec(dlBaseMHz = 2570.0, dlOffset = 37750, duplexMode = "TDD"),
        "B39" to LteBandSpec(dlBaseMHz = 1880.0, dlOffset = 38250, duplexMode = "TDD"),
        "B40" to LteBandSpec(dlBaseMHz = 2300.0, dlOffset = 38650, duplexMode = "TDD"),
        "B41" to LteBandSpec(dlBaseMHz = 2496.0, dlOffset = 39650, duplexMode = "TDD"),
    )
}
