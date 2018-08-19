package com.omarea.shared;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.omarea.shared.model.AppConfigInfo;

public class AppConfigStore extends SQLiteOpenHelper {
    public AppConfigStore(Context context) {
        super(context, "app-settings", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("create table app_config(" +
                    "id text primary key, " +
                    "alone_light int default(0), " +
                    "light int default(-1), " +
                    "dis_notice int default(0)," +
                    "dis_button int default(0)," +
                    "gps_on int default(0)," +
                    "dis_background_run int default(0))");
        } catch (Exception ignored) {

        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public AppConfigInfo getAppConfig(String app) {
        AppConfigInfo appConfigInfo = new AppConfigInfo();
        appConfigInfo.packageName = app;
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("select * from app_config where id = ?", new String[]{app});
            if (cursor.moveToNext()) {
                appConfigInfo.aloneLight = cursor.getInt(cursor.getColumnIndex("alone_light")) == 1;
                appConfigInfo.aloneLightValue = cursor.getInt(cursor.getColumnIndex("light"));
                appConfigInfo.disNotice = cursor.getInt(cursor.getColumnIndex("dis_notice")) == 1;
                appConfigInfo.disButton = cursor.getInt(cursor.getColumnIndex("dis_button")) == 1;
                appConfigInfo.gpsOn = cursor.getInt(cursor.getColumnIndex("gps_on")) == 1;
                appConfigInfo.disBackgroundRun = cursor.getInt(cursor.getColumnIndex("dis_background_run")) == 1;
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ignored) {

        }
        return appConfigInfo;
    }

    public boolean setAppConfig(AppConfigInfo appConfigInfo) {
        SQLiteDatabase database = getWritableDatabase();
        getWritableDatabase().beginTransaction();
        try {
            database.execSQL("delete from  app_config where id = ?", new String[]{appConfigInfo.packageName});
            database.execSQL("insert into app_config(id, alone_light, light, dis_notice, dis_button, gps_on, dis_background_run) values (?, ?, ?, ?, ?, ?, ?)", new Object[]{
                    appConfigInfo.packageName,
                    appConfigInfo.aloneLight ? 1 : 0,
                    appConfigInfo.aloneLightValue,
                    appConfigInfo.disNotice ? 1 : 0,
                    appConfigInfo.disButton ? 1 : 0,
                    appConfigInfo.gpsOn ? 1 : 0,
                    appConfigInfo.disBackgroundRun ? 1 : 0
            });
            database.setTransactionSuccessful();
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            database.endTransaction();
        }
    }

    public boolean needKeyCapture () {
        boolean r = false;
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("select * from app_config where dis_button = 1", new String[]{});
            if (cursor.moveToNext()) {
                r = true;
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ignored) {

        }
        return r;
    }
}
