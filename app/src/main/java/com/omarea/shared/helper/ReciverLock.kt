package com.omarea.shared.helper

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler

/**
 * 监听屏幕开关事件
 * Created by Hello on 2018/01/23.
 */

class ReciverLock(private var callbacks: Handler) : BroadcastReceiver() {
    private var handler = Handler()
    private var lastChange = 0L
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1 == null) {
            return
        }
        when (p1.action) {
            Intent.ACTION_SCREEN_OFF -> {
                lastChange = System.currentTimeMillis()
                try {
                    callbacks.sendMessage(callbacks.obtainMessage(8))
                } catch (ex: Exception) {
                    System.out.print(">>>>>" + ex.message)
                }
            }
            Intent.ACTION_USER_PRESENT -> {
                lastChange = System.currentTimeMillis()
                try {
                    callbacks.sendMessage(callbacks.obtainMessage(7))
                } catch (ex: Exception) {
                    System.out.print(">>>>>" + ex.message)
                }
            }
            Intent.ACTION_USER_UNLOCKED,
            Intent.ACTION_SCREEN_ON -> {
                val ms = System.currentTimeMillis()
                lastChange = System.currentTimeMillis()
                try {
                    handler.postDelayed({
                        if (ms == lastChange) {
                            try {
                                val mKeyguardManager = p0!!.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                                if (!mKeyguardManager.isKeyguardLocked) {
                                    callbacks.sendMessage(callbacks.obtainMessage(7))
                                } else if (!mKeyguardManager.inKeyguardRestrictedInputMode()) {
                                    callbacks.sendMessage(callbacks.obtainMessage(7))
                                }
                            } catch (ex: Exception) {
                                System.out.print(">>>>>" + ex.message)
                            }
                        }
                    }, 5500)
                } catch (ex: Exception) {
                    System.out.print(">>>>>" + ex.message)
                }
            }
        }
    }

    companion object {
        private var reciver: ReciverLock? = null
        fun autoRegister(context: Context, callbacks: Handler) {
            if (reciver != null) {
                unRegister(context)
            }

            reciver = ReciverLock(callbacks)

            context.applicationContext.registerReceiver(reciver, IntentFilter(Intent.ACTION_SCREEN_OFF))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.applicationContext.registerReceiver(reciver, IntentFilter(Intent.ACTION_USER_UNLOCKED))
            }
            context.applicationContext.registerReceiver(reciver, IntentFilter(Intent.ACTION_SCREEN_ON))
            context.applicationContext.registerReceiver(reciver, IntentFilter(Intent.ACTION_USER_PRESENT))
        }

        fun unRegister(context: Context) {
            if (reciver == null) {
                return
            }
            context.applicationContext.unregisterReceiver(reciver)
            reciver = null
        }
    }
}
