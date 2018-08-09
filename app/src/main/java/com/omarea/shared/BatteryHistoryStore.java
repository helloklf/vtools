package com.omarea.shared;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.omarea.shared.model.AppConfigInfo;

public class BatteryHistoryStore extends SQLiteOpenHelper {
    public BatteryHistoryStore(Context context) {
        super(context, "battery-history", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(
            "create table battery_io(" +
                "time text primary key, " +
                "level int default(0), " +
                "temperature int default(-1), " +
                "status int default(0)," +
                "voltage int default(0)," +
                "io int default(0)," +
                "package text" +
            ")");
        } catch (Exception ex) {

        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public AppConfigInfo getAppConfig(String app) {
        AppConfigInfo appConfigInfo = new AppConfigInfo();
        appConfigInfo.packageName = app;
        try {
            Cursor cursor = this.getReadableDatabase().rawQuery("select * from app_config where id = ?", new String[]{app});
            if (cursor.moveToNext()) {
                appConfigInfo.aloneLight = cursor.getInt(cursor.getColumnIndex("alone_light")) == 1;
                appConfigInfo.aloneLightValue = cursor.getInt(cursor.getColumnIndex("light"));
                appConfigInfo.disNotice = cursor.getInt(cursor.getColumnIndex("dis_notice")) == 1;
                appConfigInfo.disButton = cursor.getInt(cursor.getColumnIndex("dis_button")) == 1;
                appConfigInfo.gpsOn = cursor.getInt(cursor.getColumnIndex("gps_on")) == 1;
                appConfigInfo.disBackgroundRun = cursor.getInt(cursor.getColumnIndex("dis_background_run")) == 1;
            }
            cursor.close();
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
}
