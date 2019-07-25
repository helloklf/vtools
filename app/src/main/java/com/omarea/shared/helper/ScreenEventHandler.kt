package com.omarea.shared.helper

import android.os.Handler
import android.os.Looper
import android.os.Message

/**
 * 处理屏幕开关事件的Handler
 * Created by Hello on 2018/01/23.
 */
interface ScreenEventHandler {
    fun onScreenOn()
    fun onScreenOff()
}
