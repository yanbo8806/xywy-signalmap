package com.xywy.signalmap

import android.content.Intent

object ImportUriPermissionFlags {
    fun persistableReadWriteFlags(intentFlags: Int): Int = intentFlags and (
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
}
