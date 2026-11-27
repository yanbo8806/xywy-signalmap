package com.xywy.signalmap

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

object ParcelableUriExtractor {
    @SuppressLint("NewApi")
    fun fromIntent(
        intent: Intent,
        key: String,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Uri? = extract(
        sdkInt = sdkInt,
        typedGetter = {
            intent.getParcelableUriExtraCompat(key)
        },
        legacyGetter = {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(key) as? Uri
        },
    )

    internal fun <T> extract(
        sdkInt: Int,
        typedGetter: () -> T?,
        legacyGetter: () -> T?,
    ): T? = if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        typedGetter()
    } else {
        legacyGetter()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Intent.getParcelableUriExtraCompat(key: String): Uri? =
    getParcelableExtra(key, Uri::class.java)
