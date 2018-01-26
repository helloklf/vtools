package com.omarea.shared.helper

import android.os.Handler
import android.os.Message

/**
 * Created by Hello on 2018/01/23.
 */

internal class MyHandler(private var onSceenOff: ()-> Unit, private var onSceenOn: ()-> Unit): Handler() {
    override fun handleMessage(msg: Message?) {
        super.handleMessage(msg)
        if (msg == null) {
            return
        }

        when(msg.what) {
            7 -> onSceenOn()    //屏幕已解锁
            8 -> onSceenOff()   //屏幕已关闭
        }
    }
}
