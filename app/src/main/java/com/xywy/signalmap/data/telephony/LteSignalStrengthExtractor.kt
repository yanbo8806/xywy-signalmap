package com.xywy.signalmap.data.telephony

import android.os.Build
import android.telephony.CellSignalStrengthLte
import android.telephony.SignalStrength
import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.RadioTechnology
import java.lang.reflect.Field
import java.util.Locale

internal data class LteSignalSupplement(
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val sinr: Int? = null,
    val dbm: Int? = null,
) {
    val hasAnyValue: Boolean
        get() = rsrp != null || rsrq != null || sinr != null || dbm != null
}

internal object LteSignalStrengthExtractor {
    fun fromSignalStrength(signalStrength: SignalStrength): LteSignalSupplement? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fromPublicApi(signalStrength).takeIf { it.hasAnyValue }
        } else {
            fromReflectionFields(signalStrength)
        }
    }

    fun fromReflectionFields(signalStrength: Any): LteSignalSupplement? {
        val fields = signalStrength.javaClass.allFields()
        val supplement = LteSignalSupplement(
            rsrp = fields.findIntField(signalStrength) { name ->
                name.contains("lte") && name.contains("rsrp")
            }?.normalizeNegativeDb(),
            rsrq = fields.findIntField(signalStrength) { name ->
                name.contains("lte") && name.contains("rsrq")
            }?.normalizeNegativeDb(),
            sinr = fields.findIntField(signalStrength) { name ->
                name.contains("lte") && name.contains("rssnr")
            }?.let { SignalValueNormalizer.lteSinr(it, 28) },
        )
        return supplement.copy(
            rsrp = supplement.rsrp?.let(SignalValueNormalizer::lteRsrp),
            rsrq = supplement.rsrq?.let(SignalValueNormalizer::lteRsrq),
            // CellSignal.dbm is RSRP for LTE. Do not mix legacy RSSI/ASU into it.
            dbm = supplement.rsrp?.let(SignalValueNormalizer::lteRsrp),
        ).takeIf { it.hasAnyValue }
    }

    fun applySupplement(
        cell: CellSignal,
        supplement: LteSignalSupplement?,
    ): CellSignal {
        if (supplement == null || cell.radioTechnology != RadioTechnology.LTE) return cell
        return cell.copy(
            rsrp = supplement.rsrp,
            rsrq = supplement.rsrq,
            sinr = supplement.sinr,
            dbm = supplement.dbm,
        )
    }

    fun applyToServingLte(
        cells: List<CellSignal>,
        supplement: LteSignalSupplement?,
    ): List<CellSignal> {
        if (supplement == null || !supplement.hasAnyValue) return cells
        val serving = TelephonyCellSelection.selectServing(cells)
            ?.takeIf { it.radioTechnology == RadioTechnology.LTE }
            ?: return cells
        var patched = false
        return cells.map { cell ->
            if (!patched && cell === serving) {
                patched = true
                applySupplement(cell, supplement)
            } else {
                cell
            }
        }
    }

    private fun fromPublicApi(signalStrength: SignalStrength): LteSignalSupplement {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return LteSignalSupplement()
        val lte = runCatching {
            signalStrength.getCellSignalStrengths(CellSignalStrengthLte::class.java).singleOrNull()
        }.getOrNull() ?: return LteSignalSupplement()
        return LteSignalSupplement(
            rsrp = SignalValueNormalizer.lteRsrp(lte.rsrp),
            rsrq = SignalValueNormalizer.lteRsrq(lte.rsrq),
            sinr = SignalValueNormalizer.lteSinr(lte.rssnr, Build.VERSION.SDK_INT),
            dbm = SignalValueNormalizer.lteRsrp(lte.dbm),
        )
    }

    private fun Class<*>.allFields(): List<Field> {
        val result = mutableListOf<Field>()
        var current: Class<*>? = this
        while (current != null && current != Any::class.java) {
            result += current.declaredFields
            current = current.superclass
        }
        return result
    }

    private inline fun List<Field>.findIntField(
        instance: Any,
        crossinline predicate: (String) -> Boolean,
    ): Int? = firstNotNullOfOrNull { field ->
        val lowerName = field.name.lowercase(Locale.US)
        if (!predicate(lowerName)) return@firstNotNullOfOrNull null
        runCatching {
            field.isAccessible = true
            (field.get(instance) as? Int)?.validSignalValue()
        }.getOrNull()
    }

    private fun Int.validSignalValue(): Int? =
        takeUnless { it == Int.MAX_VALUE || it == Int.MIN_VALUE }

    private fun Int.normalizeNegativeDb(): Int? =
        validSignalValue()?.let { if (it > 0) -it else it }

}
