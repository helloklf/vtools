package com.omarea.store;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AutoSkipConfigStore extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;

    public AutoSkipConfigStore(Context context) {
        super(context, "auto_skip_config", null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("create table auto_skip_ids(" +
                    "activity text primary key, " +
                    "viewId text" +
                    ")");
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public boolean addConfig(String activity, String viewId) {
        try {
            SQLiteDatabase database = getWritableDatabase();
            database.execSQL("insert into auto_skip_ids(activity, viewId) values (?, ?)", new Object[]{
                    activity,
                    viewId
            });
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String getSkipViewId(String activity) {
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            final Cursor cursor = sqLiteDatabase.rawQuery("select viewId from auto_skip_ids where activity = ?", new String[]{
                    activity
            });
            try {
                if (cursor.moveToNext()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
                sqLiteDatabase.close();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public boolean clearAll() {
        try {
            SQLiteDatabase database = getWritableDatabase();
            database.execSQL("delete from auto_skip_ids", new String[]{});
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}

