package com.xywy.signalmap

import android.content.Intent
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportUriPermissionFlagsTest {
    @Test
    fun `persistable permission request keeps only read and write flags`() {
        val intentFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION

        val result = ImportUriPermissionFlags.persistableReadWriteFlags(intentFlags)

        assertEquals(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            result,
        )
    }
}
