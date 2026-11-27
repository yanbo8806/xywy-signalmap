package com.xywy.signalmap.data.telephony

import android.os.Build
import android.telephony.CellSignalStrengthNr
import android.telephony.SignalStrength
import com.xywy.signalmap.domain.model.RadioTechnology

internal data class ServingSignalUpdate(
    val technology: RadioTechnology,
    val rsrp: Int?,
    val rsrq: Int?,
    val sinr: Int?,
    val dbm: Int?,
) {
    companion object {
        fun extract(signal: SignalStrength): List<ServingSignalUpdate> = buildList {
            LteSignalStrengthExtractor.fromSignalStrength(signal)?.let {
                add(ServingSignalUpdate(RadioTechnology.LTE, it.rsrp, it.rsrq, it.sinr, it.dbm))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                signal.getCellSignalStrengths(CellSignalStrengthNr::class.java).singleOrNull()?.let { nr ->
                    add(ServingSignalUpdate(
                        RadioTechnology.NR,
                        SignalValueNormalizer.nrRsrp(nr.ssRsrp),
                        SignalValueNormalizer.nrRsrq(nr.ssRsrq),
                        SignalValueNormalizer.nrSinr(nr.ssSinr),
                        SignalValueNormalizer.nrRsrp(nr.dbm),
                    ))
                }
            }
        }
    }
}
