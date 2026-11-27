package com.xywy.signalmap.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Keep tools full-width and reserve a side HUD only when it leaves room for the panels. */
@Composable
internal fun MapOverlayLayout(
    panelsExpanded: Boolean,
    modifier: Modifier = Modifier,
    toolbar: @Composable () -> Unit,
    panels: @Composable () -> Unit,
    signalHud: (@Composable () -> Unit)? = null,
    compactSignalHud: (@Composable () -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val showSideHud = !panelsExpanded || maxWidth >= 600.dp
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            toolbar()
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.Top,
            ) {
                if (panelsExpanded) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!showSideHud) compactSignalHud?.invoke()
                        panels()
                    }
                }
                if (showSideHud && signalHud != null) {
                    Column(
                        modifier = Modifier
                            .width(156.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        signalHud()
                    }
                }
            }
        }
    }
}
