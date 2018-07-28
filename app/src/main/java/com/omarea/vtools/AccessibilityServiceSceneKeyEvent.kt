package com.omarea.vtools

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.omarea.shared.SpfConfig


/**
 * Created by helloklf on 2016/8/27.
 */
class AccessibilityServiceSceneKeyEvent : AccessibilityService() {
    override fun onInterrupt() {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private var eventHandlers: HashMap<Int, buttonEventHandler> = HashMap()
    private lateinit var sharedPreferences: SharedPreferences
    private var floatVitualTouchBar: FloatVitualTouchBar? = null

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = this.getSharedPreferences(SpfConfig.KEY_EVENT_SPF, Context.MODE_PRIVATE)
        sharedPreferences.registerOnSharedPreferenceChangeListener({ sharedPreferences, key ->
            updateKeyEventProcess()
        })
        updateKeyEventProcess()
        floatVitualTouchBar = FloatVitualTouchBar(this)
    }

    private fun updateKeyEventProcess() {
        try {
            for (item in sharedPreferences.all.keys) {
                var keyCode = Int.MIN_VALUE
                try {
                    keyCode = item.substring(0, item.indexOf("_")).toInt()
                } catch (ex: Exception) {
                }
                if (keyCode != Int.MIN_VALUE && !eventHandlers.containsKey(keyCode))
                    eventHandlers.put(keyCode, buttonEventHandler(this, keyCode, sharedPreferences))
            }
        } catch (ex: Exception) {
            Toast.makeText(applicationContext, ex.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        //如果返回true，就会导致其他应用接收不到事件了，但是对KeyEvent的修改是不会分发到其他应用中的！
        if (eventHandlers.containsKey(event.keyCode)) {
            val stopEvent = eventHandlers[event.keyCode]!!.onEvent(event)
            return stopEvent
        }
        return false
    }

    private class buttonEventHandler(private var accessibilityService: AccessibilityService, private var keyCode: Int, private var spf: SharedPreferences) {
        private var downTime: Long = -1
        private var downTimeDefault: Long = -1
        private var longClickTime: Long = 300;
        private var handler = Handler()

        private fun onLongClick() {
            //TODO:读取配置，看看当前键码的长按设置为啥了
            val overrideKeyCode = spf.getInt("${keyCode}_long_click", Int.MIN_VALUE)
            //如果重写了事件
            if (overrideKeyCode != Int.MIN_VALUE) {
                downTime = downTimeDefault
                accessibilityService.performGlobalAction(overrideKeyCode)
            }
        }

        private fun onShortClick(): Boolean {
            //TODO: 读取配置，看当前键码短按设置为啥了
            val overrideKeyCode = spf.getInt("${keyCode}_click", Int.MIN_VALUE)
            //如果重写了事件
            if (overrideKeyCode != Int.MIN_VALUE) {
                downTime = downTimeDefault
                accessibilityService.performGlobalAction(overrideKeyCode)
                return true
            }
            return false
        }

        private fun startTimer() {
            val currentDownTime = downTime
            handler.postDelayed({
                if (downTime == currentDownTime) {
                    onLongClick()
                }
            }, longClickTime)
        }

        private fun onDown(event: KeyEvent): Boolean {
            downTime = event.eventTime
            startTimer()
            val overrideKeyCode = spf.getInt("${keyCode}_click", Int.MIN_VALUE)
            if (overrideKeyCode != Int.MIN_VALUE) {
                return true
            }
            val overrideKeyCode2 = spf.getInt("${keyCode}_long_click", Int.MIN_VALUE)
            if (overrideKeyCode2 != Int.MIN_VALUE) {
                return true
            }
            return false
        }

        private fun onUp(event: KeyEvent): Boolean {
            var stopEvent = false
            if (event.downTime == downTime) {
                //如果按住时间达到长按设定时长
                if (downTime != downTimeDefault && event.eventTime - event.downTime >= longClickTime) {
                    //TODO:读取配置，看看当前键码的长按设置为啥了
                    val overrideKeyCode = spf.getInt("${keyCode}_long_click", Int.MIN_VALUE)
                    //如果重写了事件
                    if (overrideKeyCode != Int.MIN_VALUE) {
                        stopEvent = true
                    }
                }
                //如果不是长按那就是短按咯
                else {
                    stopEvent = onShortClick()
                }
            } else {
                stopEvent = true
                //居然还有没有经过down就到了up的事件？也许可能会有吧，反正这么写做容错好了
            }
            downTime = downTimeDefault;
            return stopEvent
        }

        fun onEvent(event: KeyEvent): Boolean {
            if (event.action == ACTION_DOWN) {
                return onDown(event)
            } else if (event.action == ACTION_UP) {
                return onUp(event)
            }
            return false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatVitualTouchBar != null) {
            floatVitualTouchBar!!.hidePopupWindow()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if ((event.getEventType() == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED)) {
            if ("com.android.systemui".equals(event.getPackageName())) {

            } else {

            }
        }
    }
}