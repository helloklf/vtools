package com.omarea.store;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.omarea.model.BatteryAvgStatus;
import com.omarea.model.BatteryStatus;

import java.util.ArrayList;

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
                            "level int default(-1), " +
                            "temperature REAL default(-1), " +
                            "status int default(-1)," +
                            "mode text," +
                            "io int default(-1)," +
                            "package text" +
                            ")");
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public boolean insertHistory(BatteryStatus batteryStatus) {
        SQLiteDatabase database = getWritableDatabase();
        getWritableDatabase().beginTransaction();
        try {
            database.execSQL("insert into battery_io(time, level, temperature, status, mode, io, package) values (?, ?, ?, ?, ?, ?, ?)", new Object[]{
                    "" + batteryStatus.time,
                    batteryStatus.level,
                    batteryStatus.temperature,
                    batteryStatus.status,
                    batteryStatus.mode,
                    batteryStatus.io,
                    batteryStatus.packageName
            });
            database.setTransactionSuccessful();
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            database.endTransaction();
        }
    }

    public ArrayList<BatteryAvgStatus> getAvgData(long timeStart) {
        try {
            SQLiteDatabase sqLiteDatabase = getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("select avg(io) AS io, avg(temperature) as temperature, package, mode, count(io) from battery_io group by package, mode", new String[]{});
            ArrayList<BatteryAvgStatus> data = new ArrayList<>();
            while (cursor.moveToNext()) {
                BatteryAvgStatus batteryAvgStatus = new BatteryAvgStatus();
                batteryAvgStatus.io = cursor.getInt(0);
                batteryAvgStatus.temperature = cursor.getInt(1);
                batteryAvgStatus.packageName = cursor.getString(2);
                batteryAvgStatus.mode = cursor.getString(3);
                batteryAvgStatus.count = cursor.getInt(4);
                data.add(batteryAvgStatus);
            }
            cursor.close();
            return data;
        } catch (Exception ex) {
            Log.e("query-data-base", "" + ex.getMessage());
        }
        return new ArrayList<>();
    }

    public boolean clearData() {
        try {
            SQLiteDatabase database = getWritableDatabase();
            database.delete("battery_io", " 1 =1", new String[]{});
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
