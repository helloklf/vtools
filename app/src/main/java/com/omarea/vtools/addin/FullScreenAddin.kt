package com.omarea.vtools.addin

import android.app.Activity
import android.app.AlertDialog
import android.widget.Toast
import com.omarea.common.shared.FileWrite
import com.omarea.common.shared.MagiskExtend
import com.omarea.common.ui.DialogHelper
import com.omarea.utils.CommonCmds

/**
 * Created by Hello on 2017/11/01.
 */

class FullScreenAddin(private var context: Activity) : AddinBase(context) {
    fun hideNavgationBar() {
        FileWrite.writePrivateFile(context.assets, "framework-res", "framework-res", context)
        if (MagiskExtend.moduleInstalled()) {
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setTitle("注意")
                    .setMessage("此操作会写入/system/media/theme/default/framework-res，需要重启才能生效。如需还原，请到Magisk助手删除对应位置的文件")
                    .setPositiveButton("知道了", { _, _ ->
                        MagiskExtend.replaceSystemFile("/system/media/theme/default/framework-res", "${FileWrite.getPrivateFileDir(context)}/framework-res")
                        Toast.makeText(context, "已通过Magisk更改参数，请重启手机~", Toast.LENGTH_SHORT).show()
                    }))
        } else {
            command = StringBuilder()
                    .append(CommonCmds.MountSystemRW)
                    .append("cp ${FileWrite.getPrivateFileDir(context)}/framework-res /system/media/theme/default/framework-res\n")
                    .append("chmod 0755 /system/media/theme/default/framework-res\n")
                    .toString()
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setTitle("注意")
                    .setMessage("此操作会写入/system/media/theme/default/framework-res，需要重启才能生效。如需还原，则需删除/system/media/theme/default/framework-res！")
                    .setPositiveButton("知道了", { _, _ ->
                    }))
            super.run()
        }
    }

    fun fullScreen() {
        val arr = arrayOf("全部隐藏", "隐藏导航栏", "隐藏状态栏", "恢复默认", "移走导航栏（试验）", "MIUI(专属)去导航栏")
        var index = 0
        DialogHelper.animDialog(AlertDialog.Builder(context)
                .setTitle("请选择操作")
                .setSingleChoiceItems(arr, index, { _, which ->
                    index = which
                })
                .setNegativeButton("确定", { _, _ ->
                    when (index) {
                        0 -> execShell("wm overscan reset;settings put global policy_control immersive.full=apps,-android,-com.android.systemui,-com.tencent.mobileqq,-com.tencent.tim,-com.tencent.mm,-com.tencent.tim,-com.tencent.tim;")
                        1 -> execShell("wm overscan reset;settings put global policy_control immersive.navigation=*")
                        2 -> execShell("wm overscan reset;settings put global policy_control immersive.status=apps,-android,-com.android.systemui,-com.tencent.mobileqq,-com.tencent.tim,-com.tencent.mm,-com.tencent.tim,-com.tencent.tim;")
                        3 -> execShell("wm overscan reset;settings put global policy_control null;")
                        4 -> execShell("wm overscan 0,0,0,-${getNavHeight()};")
                        5 -> {
                            hideNavgationBar()
                        }
                    }
                    if (index < 4)
                        Toast.makeText(context, "腾讯系列聊天软件不兼容状态栏隐藏，自动加入忽略列表！", Toast.LENGTH_LONG).show()
                }))
    }

    fun getNavHeight(): Int {
        val resources = context.getResources();
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        val height = resources.getDimensionPixelSize(resourceId);
        return height + 1
    }
}
