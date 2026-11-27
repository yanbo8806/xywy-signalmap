package com.xywy.signalmap.data.importer

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ImportFileMetadataTest {
    @Test
    fun `provider failures fall back to filename without crashing the caller`() {
        ShadowContentResolver.registerProviderInternal("broken.import", object : MetadataProvider() {
            override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
                throw SecurityException("read grant revoked")
            }
            override fun getType(uri: Uri): String = throw IllegalArgumentException("metadata unavailable")
        })
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        val uri = Uri.parse("content://broken.import/path/layer.kml")
        assertEquals("layer.kml", ImportFileMetadata.displayName(resolver, uri))
        assertNull(ImportFileMetadata.mimeType(resolver, uri))
    }

    @Test
    fun `provider display name is used and cursor is closed`() {
        val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply { addRow(arrayOf("工参.xlsx")) }
        ShadowContentResolver.registerProviderInternal("valid.import", object : MetadataProvider() {
            override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor = cursor
        })
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        assertEquals("工参.xlsx", ImportFileMetadata.displayName(resolver, Uri.parse("content://valid.import/123")))
        assertTrue(cursor.isClosed)
    }

    private abstract class MetadataProvider : ContentProvider() {
        override fun onCreate() = true
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }
}
