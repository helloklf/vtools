package com.omarea.scene_mode

import android.view.accessibility.AccessibilityEvent

class WindowAnalyseEventFilter {
    private var lastOriginEventTime = 0L
    fun filter(event: AccessibilityEvent): Boolean {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            return false
        }

        val t = event.eventTime
        if (lastOriginEventTime != t && t > lastOriginEventTime) {
            lastOriginEventTime = t
            return true
        }
        return false
    }
}