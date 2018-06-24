package com.omarea.shared

import android.content.Context
import android.database.sqlite.SQLiteDatabase

/**
 * Created by Hello on 2018/06/24.
 */

class BatteryStorage(private var context: Context) {
    private var db:SQLiteDatabase? = null
    private fun createDB(): Boolean {
        try {
            db = SQLiteDatabase.openOrCreateDatabase( context.filesDir.absolutePath + "./databases/vtools.db",null)
            return true
        } catch (ex: Exception) {
            return false
        }
    }

    private fun createTable(): Boolean {
        if (db == null) {
            if (!createDB()) {
                return false
            }
        }
        try {
            db!!.execSQL("create table if not exists usertable(_id integer primary key autoincrement, io text, capacity real, temp real)")
            return true
        } catch (ex: Exception) {
            return false
        }
    }
}
