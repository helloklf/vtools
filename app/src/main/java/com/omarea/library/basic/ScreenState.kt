package com.omarea.library.basic

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager

class ScreenState(private var context: Context) {
    private val mKeyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private val screenOffStatus = intArrayOf(
            Display.STATE_OFF,
            Display.STATE_DOZE,
            Display.STATE_DOZE_SUSPEND,
            Display.STATE_ON_SUSPEND
    ).toTypedArray()

    fun isScreenLocked(): Boolean {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        if (screenOffStatus.contains(display.state)) {
            return true
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mKeyguardManager.isDeviceLocked || mKeyguardManager.isKeyguardLocked
        } else {
            mKeyguardManager.inKeyguardRestrictedInputMode() || mKeyguardManager.isDeviceLocked || mKeyguardManager.isKeyguardLocked
        }
    }

    fun isScreenOn(): Boolean {
        return !isScreenLocked()
    }
}
