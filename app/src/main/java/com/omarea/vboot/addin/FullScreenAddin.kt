package com.omarea.vboot.addin

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast

/**
 * Created by Hello on 2017/11/01.
 */

class FullScreenAddin(private var context: Context) : AddinBase(context) {
    fun fullScreen() {
        val arr = arrayOf("全部隐藏", "隐藏导航栏", "隐藏状态栏", "恢复默认")
        var index = 0
        AlertDialog.Builder(context)
                .setTitle("请选择操作")
                .setSingleChoiceItems(arr, index, { _, which ->
                    index = which
                })
                .setNegativeButton("确定", { _, _ ->
                    when (index) {
                        0 -> execShell("settings put global policy_control immersive.full=apps,-android,-com.android.systemui,-com.tencent.mobileqq,-com.tencent.tim,-com.tencent.mm,-com.tencent.tim,-com.tencent.tim;")
                        1 -> execShell("settings put global policy_control immersive.navigation=*")
                        2 -> execShell("settings put global policy_control immersive.status=apps,-android,-com.android.systemui,-com.tencent.mobileqq,-com.tencent.tim,-com.tencent.mm,-com.tencent.tim,-com.tencent.tim;")
                        3 -> execShell("settings put global policy_control null;")
                    }
                    Toast.makeText(context, "腾讯系列聊天软件不兼容状态栏隐藏，自动加入忽略列表！", Toast.LENGTH_LONG).show()
                })
                .create().show()
    }
}
