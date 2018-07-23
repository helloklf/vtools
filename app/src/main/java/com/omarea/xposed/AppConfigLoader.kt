package com.omarea.xposed

import android.content.ContentResolver
import android.net.Uri
import com.omarea.AppConfigInfo
import de.robv.android.xposed.XposedBridge

class AppConfigLoader {
    fun getAppConfig(packageName: String, resolver: ContentResolver): AppConfigInfo? {
        val appConfigInfo = AppConfigInfo()
        appConfigInfo.packageName = packageName
        try {
            val uri = Uri.parse("content://com.omarea.vtools.appconfig")
            // uri columns ...
            val cursor = resolver.query(uri, arrayOf(packageName), null, null, null)

            if (cursor == null)
            {
                XposedBridge.log("APP设置返回空")
                return appConfigInfo;
            }
            try {
                if (cursor.moveToNext()) {
                    appConfigInfo.dpi = cursor.getInt(cursor.getColumnIndex("dpi"))
                    appConfigInfo.excludeRecent = cursor.getInt(cursor.getColumnIndex("exclude_recent")) == 1
                    appConfigInfo.smoothScroll = cursor.getInt(cursor.getColumnIndex("smooth_scroll")) == 1
                }
                XposedBridge.log("获取APP设置成功！")
            } catch (ignored: Exception) {
            } finally {
                cursor.close()
            }
        } catch (ex: Exception) {
            XposedBridge.log("获取APP设置失败" + ex.message)
        }
        return appConfigInfo
    }
}
