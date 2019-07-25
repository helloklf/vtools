package com.omarea.scene_mode

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager

class ScreenState(private var context: Context) {
    fun isScreenLocked(): Boolean {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        if (display.state != Display.STATE_ON) {
            return true
        }

        val mKeyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mKeyguardManager.inKeyguardRestrictedInputMode() || mKeyguardManager.isDeviceLocked || mKeyguardManager.isKeyguardLocked
        } else {
            mKeyguardManager.inKeyguardRestrictedInputMode() || mKeyguardManager.isKeyguardLocked
        }
    }

    fun isScreenOn(): Boolean {
        return !isScreenLocked()
    }
}
