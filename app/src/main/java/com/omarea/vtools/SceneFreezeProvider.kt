package com.omarea.vtools

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import com.omarea.Scene
import com.omarea.scene_mode.SceneMode
import com.omarea.store.SpfConfig

class SceneFreezeProvider : ContentProvider() {
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    private var config: SharedPreferences? = null
    private fun allowXposedOpen(): Boolean {
        if (config == null) {
            config = Scene.context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        }
        return config!!.getBoolean(SpfConfig.GLOBAL_SPF_FREEZE_XPOSED_OPEN, false)
    }

    val whiteList = arrayOf(
            "com.android.quicksearchbox", // 搜索
            "com.android.settings", // 设置
            // nova
            "com.teslacoilsw.launcher",
            // poco
            "com.mi.android.globallauncher",
            // miui
            "com.miui.home",
            // lawnchair 测试版
            "ch.deletescape.lawnchair.ci",
            // 一加桌面
            "net.oneplus.launcher",
            // 一加氢桌面
            "net.oneplus.h2launcher",
            // 一加hydrogen桌面
            "com.oneplus.hydrogen.launcher",
            // 微软桌面
            "com.microsoft.launcher",
            // LineageOS桌面
            "org.lineageos.trebuchet",
            // 魔趣桌面
            "org.mokee.lawnchair",
            // Pixel 启动器
            "com.google.android.apps.nexuslauncher")

    override fun getType(uri: Uri): String? {
        return "application/json"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (values != null && values.containsKey("packageName") && values.containsKey("source")) {
            val packageName = values.get("packageName").toString()
            val source = values.get("source").toString()
            if (whiteList.contains(source) || allowXposedOpen()) {
                SceneMode.unfreezeApp(packageName)
            }
            return uri;
        }
        return null
    }

    override fun onCreate(): Boolean {
        return true;
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        return null
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?): Int {
        return 0
    }
}
