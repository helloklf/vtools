package com.omarea.vtools.addin

import android.app.Activity
import android.app.AlertDialog
import com.omarea.common.ui.DialogHelper

/**
 * Created by Hello on 2017/11/01.
 */

class FullScreenAddin(private var context: Activity) : AddinBase(context) {

    fun fullScreen() {
        val arr = arrayOf("全部隐藏", "隐藏导航栏", "隐藏状态栏", "恢复默认", "移走导航栏（试验）", "MIUI(专属)去导航栏")
        var index = 0
        DialogHelper.animDialog(AlertDialog.Builder(context)
                .setTitle("请选择操作")
                .setSingleChoiceItems(arr, index) { _, which ->
                    index = which
                }
                .setNegativeButton("确定") { _, _ ->
                    when (index) {
                        0 -> execShell("wm overscan reset;settings put global policy_control immersive.full=apps,-android,-com.android.systemui")
                        1 -> execShell("wm overscan reset;settings put global policy_control immersive.navigation=*")
                        2 -> execShell("wm overscan reset;settings put global policy_control immersive.status=apps,-android,-com.android.systemui")
                        3 -> execShell("wm overscan reset;settings put global policy_control null")
                        4 -> execShell("wm overscan 0,0,0,-${getNavHeight()}")
                        5 -> {
                            DialogHelper.helpInfo(context, "功能迁移提示", "该功能已迁移到 [附加功能] - [全部] - [MIUI专属] 中")
                            return@setNegativeButton
                        }
                    }
                })
    }

    fun getNavHeight(): Int {
        val resources = context.getResources();
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        val height = resources.getDimensionPixelSize(resourceId);
        return height + 1
    }
}
