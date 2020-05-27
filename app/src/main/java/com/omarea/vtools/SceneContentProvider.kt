package com.omarea.vtools

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.omarea.data_collection.EventBus
import com.omarea.data_collection.EventType
import com.omarea.data_collection.GlobalStatus
import com.omarea.scene_mode.ModeSwitcher

class SceneContentProvider : ContentProvider() {
    companion object {
        private val MATCHER = UriMatcher(UriMatcher.NO_MATCH)
        private val PERSONS = 1
        private val PERSON = 2

        init {
            MATCHER.addURI("com.omarea.vtools.SceneContentProvider", "insert", PERSONS)
            //* 根据pesonid来删除记录
            MATCHER.addURI("com.omarea.vtools.SceneContentProvider", "delete", PERSON)
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        if (selection != null && selection.equals("packageName") && selectionArgs != null) {
            val packageName = selectionArgs.first().toString();
            Log.d("SceneContentProvider × ", packageName)
            return 1
        }
        return 0
    }

    override fun getType(uri: Uri): String? {
        return "application/json"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (values != null && values.containsKey("packageName")) {
            val packageName = values.get("packageName").toString();

            // com.miui.freeform 是miui的应用多窗口（快速回复、游戏模式QQ微信小窗口）管理器
            if (packageName == "android" || packageName == "com.android.systemui" || packageName == "com.miui.freeform" || packageName == "com.omarea.gesture") {
                //
            } else {
                ModeSwitcher().setCurrentPowercfgApp(packageName);
                Log.d("SceneContentProvider ->", packageName)

                GlobalStatus.lastPackageName = packageName
                EventBus.publish(EventType.APP_SWITCH);

                return uri
            }
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
