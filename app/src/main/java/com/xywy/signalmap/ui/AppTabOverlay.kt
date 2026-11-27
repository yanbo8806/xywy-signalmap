package com.xywy.signalmap.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier

/** Opaque, touch-blocking pages over the retained map, with per-tab saved state. */
@Composable
internal fun AppTabOverlay(
    tab: AppTab?,
    content: @Composable (AppTab) -> Unit,
) {
    val tabStateHolder = rememberSaveableStateHolder()
    Crossfade(
        targetState = tab,
        animationSpec = tween(durationMillis = 200),
        label = "overlay-tab",
    ) { overlayTab ->
        if (overlayTab != null) {
            tabStateHolder.SaveableStateProvider(overlayTab) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    content(overlayTab)
                }
            }
        }
    }
}
