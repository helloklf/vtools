package com.omarea.store;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.omarea.model.BatteryAvgStatus;
import com.omarea.model.BatteryStatus;

import java.util.ArrayList;

public class BatteryHistoryStore extends SQLiteOpenHelper {
    public BatteryHistoryStore(Context context) {
        super(context, "battery-history", null, 2);
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
        if (oldVersion == 1) {
            db.delete("battery_io", " 1 = 1", new String[]{});
        }
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

    public int getMaxTemperature() {
        SQLiteDatabase database = getWritableDatabase();
        getWritableDatabase().beginTransaction();
        try {
            Cursor cursor = database.rawQuery("select max(temperature) AS io from battery_io", new String[]{});
            ArrayList<BatteryAvgStatus> data = new ArrayList<>();
            int temperature = 0;
            while (cursor.moveToNext()) {
                temperature = cursor.getInt(0);
            }
            cursor.close();
            return temperature;
        } catch (Exception ignored) {
        } finally {
            database.endTransaction();
        }
        return 0;
    }

    public int getMaxIO(int batteryStatus) {
        SQLiteDatabase database = getWritableDatabase();
        getWritableDatabase().beginTransaction();
        try {
            Cursor cursor = database.rawQuery("select max(io) AS io from battery_io where status = ? ", new String[]{
                    "" + batteryStatus
            });
            int io = 0;
            while (cursor.moveToNext()) {
                io = cursor.getInt(0);
            }
            cursor.close();
            return io;
        } catch (Exception ignored) {
        } finally {
            database.endTransaction();
        }
        return 0;
    }

    public int getMinIO(int batteryStatus) {
        SQLiteDatabase database = getWritableDatabase();
        getWritableDatabase().beginTransaction();
        try {
            Cursor cursor = database.rawQuery("select min(io) AS io from battery_io where status = ? ", new String[]{
                    "" + batteryStatus
            });
            int io = 0;
            while (cursor.moveToNext()) {
                io = cursor.getInt(0);
            }
            cursor.close();
            return io;
        } catch (Exception ignored) {
        } finally {
            database.endTransaction();
        }
        return 0;
    }

    public ArrayList<BatteryAvgStatus> getAvgData(int batteryStatus) {
        try {
            SQLiteDatabase sqLiteDatabase = getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery(
                    "select * from (select avg(io) AS io, avg(temperature) as avg, min(temperature) as min, max(temperature) as max, package, mode, count(io) from battery_io where status = ? group by package, mode) r order by io",
                    new String[]{
                            "" + batteryStatus
                    });
            ArrayList<BatteryAvgStatus> data = new ArrayList<>();
            while (cursor.moveToNext()) {
                BatteryAvgStatus batteryAvgStatus = new BatteryAvgStatus();
                batteryAvgStatus.io = cursor.getInt(0);
                batteryAvgStatus.avgTemperature = cursor.getInt(1);
                batteryAvgStatus.minTemperature = cursor.getInt(2);
                batteryAvgStatus.maxTemperature = cursor.getInt(3);
                batteryAvgStatus.packageName = cursor.getString(4);
                batteryAvgStatus.mode = cursor.getString(5);
                batteryAvgStatus.count = cursor.getInt(6);
                data.add(batteryAvgStatus);
            }
            cursor.close();
            return data;
        } catch (Exception ex) {
        }
        return new ArrayList<>();
    }

    public boolean clearData() {
        try {
            SQLiteDatabase database = getWritableDatabase();
            database.delete("battery_io", " 1 = 1", new String[]{});
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
