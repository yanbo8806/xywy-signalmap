package com.xywy.signalmap.ui.map

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MapRuntimeValueTest {
    @Test
    fun `registering map callbacks and cached source data does not schedule another UI update`() {
        val observer = SnapshotStateObserver { it() }
        val visibleState = mutableStateOf(0)
        val listener = MapRuntimeValue<Any?>(null)
        val sourceCache = MapRuntimeValue<Any?>(null)
        var invalidations = 0
        val scope = Any()
        observer.start()
        try {
            observer.observeReads(scope, { _: Any -> invalidations++ }) {
                visibleState.value
                listener.value
                sourceCache.value
            }
            repeat(1_000) {
                val next = Any()
                listener.value = next
                sourceCache.value = next
                assertSame(next, listener.value)
                Snapshot.sendApplyNotifications()
            }
            assertEquals(0, invalidations, "Map callback registration must not create an update feedback loop")
            visibleState.value = 1
            Snapshot.sendApplyNotifications()
            assertEquals(1, invalidations, "Actual UI state must still update normally")
        } finally { observer.stop(); observer.clear() }
    }
}
