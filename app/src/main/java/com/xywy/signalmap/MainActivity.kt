package com.xywy.signalmap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.xywy.signalmap.data.importer.ImportFileMetadata
import com.xywy.signalmap.ui.AppRoot
import com.xywy.signalmap.ui.theme.SignalMapTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory(application as SignalMapApplication)
    }
    private val importIntentTracker = ImportIntentTracker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignalMapTheme {
                AppRoot(viewModel = viewModel)
            }
        }
        handleImportIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleImportIntent(intent)
    }

    private fun handleImportIntent(intent: Intent?) {
        if (intent == null) return
        val uri = resolveImportUri(intent) ?: return
        if (!importIntentTracker.shouldHandle(
                intentToken = System.identityHashCode(intent),
                alreadyHandled = intent.getBooleanExtra(EXTRA_IMPORT_HANDLED, false),
                hasImportUri = true,
            )
        ) {
            return
        }

        val hasReadGrant = (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
        if (hasReadGrant) {
            val persistableFlags = ImportUriPermissionFlags.persistableReadWriteFlags(intent.flags)
            runCatching { contentResolver.takePersistableUriPermission(uri, persistableFlags) }
        }

        val displayName = ImportFileMetadata.displayName(contentResolver, uri)
        intent.putExtra(EXTRA_IMPORT_HANDLED, true)
        viewModel.prepareImportFile(contentResolver, uri, displayName, intent.type ?: ImportFileMetadata.mimeType(contentResolver, uri))
    }

    private fun resolveImportUri(intent: Intent): Uri? {
        return when {
            intent.action == Intent.ACTION_VIEW -> intent.data
            else -> intent.data ?: ParcelableUriExtractor.fromIntent(intent, Intent.EXTRA_STREAM)
        }
    }

    private companion object {
        const val EXTRA_IMPORT_HANDLED = "_import_handled"
    }
}
