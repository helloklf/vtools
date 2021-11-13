package com.omarea.vtools.addin

import com.omarea.common.model.SelectItem
import com.omarea.common.ui.DialogItemChooser
import com.omarea.vtools.activities.ActivityBase

/**
 * Created by Hello on 2017/11/01.
 */

class Immersive(private var context: ActivityBase) : AddinBase(context) {
    fun fullScreen() {
        DialogItemChooser(context.themeMode.isDarkMode, ArrayList<SelectItem>().apply {
            add(SelectItem().apply {
                title = "全部隐藏"
                value = "wm overscan reset;settings put global policy_control immersive.full=apps,-android,-com.android.systemui"
            })
            add(SelectItem().apply {
                title = "隐藏导航栏"
                value = "wm overscan reset;settings put global policy_control immersive.navigation=*"
            })
            add(SelectItem().apply {
                title = "隐藏状态栏"
                value = "wm overscan reset;settings put global policy_control immersive.status=apps,-android,-com.android.systemui"
            })
            add(SelectItem().apply {
                title = "恢复默认"
                value = "wm overscan reset;settings put global policy_control null"
            })
            add(SelectItem().apply {
                title = "移走导航栏(overscan)"
                value = "wm overscan 0,0,0,-${getNavHeight()}"
            })
        }, false, object : DialogItemChooser.Callback {
            override fun onConfirm(selected: List<SelectItem>, status: BooleanArray) {
                selected.firstOrNull()?.run {
                    value?.run { execShell(this) }
                }
            }
        }).setTitle("请选择操作").show(context.supportFragmentManager, "immersive-options")
    }

    fun getNavHeight(): Int {
        val resources = context.getResources();
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        val height = resources.getDimensionPixelSize(resourceId);
        return height + 1
    }
}
