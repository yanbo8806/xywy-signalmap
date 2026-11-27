package com.xywy.signalmap

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImportIntentTrackerTest {
    @Test
    fun `same uri from a new intent token should still be handled`() {
        val tracker = ImportIntentTracker()

        assertTrue(tracker.shouldHandle(intentToken = 101, alreadyHandled = false, hasImportUri = true))
        assertTrue(tracker.shouldHandle(intentToken = 202, alreadyHandled = false, hasImportUri = true))
    }

    @Test
    fun `same intent token is ignored after first handling`() {
        val tracker = ImportIntentTracker()

        assertTrue(tracker.shouldHandle(intentToken = 101, alreadyHandled = false, hasImportUri = true))
        assertFalse(tracker.shouldHandle(intentToken = 101, alreadyHandled = false, hasImportUri = true))
        assertFalse(tracker.shouldHandle(intentToken = 202, alreadyHandled = true, hasImportUri = true))
        assertFalse(tracker.shouldHandle(intentToken = 303, alreadyHandled = false, hasImportUri = false))
    }
}
