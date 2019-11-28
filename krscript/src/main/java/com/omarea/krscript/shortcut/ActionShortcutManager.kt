package com.omarea.krscript.shortcut

import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import com.omarea.krscript.model.NodeInfoBase
import java.util.*

class ActionShortcutManager(private var context: Context) {
    @TargetApi(Build.VERSION_CODES.O)
    public fun addShortcut(intent: Intent, drawable: Drawable, config: NodeInfoBase): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return createShortcutOreo(intent, drawable, config)
        } else {
            return addShortcutNougat(intent, drawable, config)
        }
    }

    public fun addShortcutNougat(intent: Intent, drawable: Drawable, config: NodeInfoBase): Boolean {
        try {
            val shortcut = Intent("com.android.launcher.action.INSTALL_SHORTCUT")
            val id = "addin_" + config.index

            //快捷方式的名称
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, config.title)//快捷方式的名字
            shortcut.putExtra("duplicate", false) // 是否允许重复创建

            //快捷方式的图标
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON, (drawable as BitmapDrawable).bitmap)

            val shortcutIntent = Intent(Intent.ACTION_MAIN)
            shortcutIntent.setClassName(context.getApplicationContext(), intent.component!!.className)
            shortcutIntent.putExtras(intent)

            shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            context.sendBroadcast(shortcut)

            return true
        } catch (ex: Exception) {
            return false
        }

    }

    @TargetApi(Build.VERSION_CODES.O)
    public fun createShortcutOreo(intent: Intent, drawable: Drawable, config: NodeInfoBase): Boolean {
        try {
            val shortcutManager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager

            if (shortcutManager.isRequestPinShortcutSupported) {
                val id = "addin_" + config.index
                val shortcutIntent = Intent(Intent.ACTION_MAIN)
                shortcutIntent.setClassName(context.getApplicationContext(), intent.component!!.className)
                shortcutIntent.putExtras(intent)

                val info = ShortcutInfo.Builder(context, id)
                        .setIcon(Icon.createWithBitmap((drawable as BitmapDrawable).bitmap))
                        .setShortLabel(config.title)
                        .setIntent(shortcutIntent)
                        .setActivity(intent.component!!) // 只有“主要”活动 - 定义过滤器Intent#ACTION_MAIN 和Intent#CATEGORY_LAUNCHER意图过滤器的活动 - 才能成为目标活动
                        .build()

                val shortcutCallbackIntent = PendingIntent.getBroadcast(context, 0, Intent(), PendingIntent.FLAG_CANCEL_CURRENT)
                if (shortcutManager.isRequestPinShortcutSupported) {
                    val items = shortcutManager.pinnedShortcuts
                    for (item in items) {
                        if (item.id == id) {
                            shortcutManager.updateShortcuts(object : ArrayList<ShortcutInfo>() {
                                init {
                                    add(info)
                                }
                            })
                            return true
                        }
                    }
                    shortcutManager.requestPinShortcut(info, shortcutCallbackIntent.intentSender)
                    return true
                } else {
                    return false
                }
            }
            return true
        } catch (ex: Exception) {
            Log.e("ActionShortcutManager", "" + ex.message)
            // Toast.makeText(context, "处理快捷方式失败" + ex.getMessage(), Toast.LENGTH_LONG).show();
            return false
        }
    }
}
