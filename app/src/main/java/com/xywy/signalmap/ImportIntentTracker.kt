package com.xywy.signalmap

internal class ImportIntentTracker {
    private var lastHandledIntentToken: Int? = null

    fun shouldHandle(
        intentToken: Int,
        alreadyHandled: Boolean,
        hasImportUri: Boolean,
    ): Boolean {
        if (alreadyHandled || !hasImportUri) return false
        if (lastHandledIntentToken == intentToken) return false
        lastHandledIntentToken = intentToken
        return true
    }
}
