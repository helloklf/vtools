package com.omarea.store;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.BatteryManager;

import com.omarea.model.BatteryAvgStatus;
import com.omarea.model.BatteryStatus;
import com.omarea.model.PowerHistory;

import java.util.ArrayList;

public class BatteryHistoryStore extends SQLiteOpenHelper {
    public BatteryHistoryStore(Context context) {
        super(context, "battery-history3", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(
                "create table battery_io(" +
                    "time text primary key, " +
                    "temperature REAL default(-1), " +
                    "status int default(-1)," +
                    "mode text," +
                    "io int default(-1)," +
                    "package text," +
                    "screen_on INTEGER," +
                    "capacity INTEGER" +
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
            database.execSQL(
                "insert into battery_io(time, temperature, status, mode, io, package, screen_on, capacity) " +
                    "values (?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{
                    "" + batteryStatus.time,
                    batteryStatus.temperature,
                    batteryStatus.status,
                    batteryStatus.mode,
                    batteryStatus.io,
                    batteryStatus.packageName,
                    batteryStatus.screenOn ? 1 : 0,
                    batteryStatus.capacity
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

    public int lastCapacity() {
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            final Cursor cursor = sqLiteDatabase.rawQuery("select TOP(1) capacity from battery_io order by time desc", new String[]{});
            try {
                if (cursor.moveToNext()) {
                    return cursor.getInt(0);
                }
            } finally {
                cursor.close();
                sqLiteDatabase.close();
            }
        } catch (Exception ignored) {
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

    public ArrayList<BatteryAvgStatus> getAvgData() {
        try {
            SQLiteDatabase sqLiteDatabase = getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery(
                "select * from (select avg(io) AS io, avg(temperature) as avg, min(temperature) as min, max(temperature) as max, package, mode, count(io) from battery_io where status in (?, ?) and package != ? group by package, mode) r order by io",
                new String[]{
                    "" + BatteryManager.BATTERY_STATUS_DISCHARGING,
                    "" + BatteryManager.BATTERY_STATUS_NOT_CHARGING,
                    ""
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
        } catch (Exception ignored) {
        }
        return new ArrayList<>();
    }

    public ArrayList<PowerHistory> getCurve() {
        ArrayList<PowerHistory> histories = new ArrayList<>();
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            final Cursor cursor = sqLiteDatabase.rawQuery(
                "select time, capacity, screen_on, status from battery_io",
                new String[]{}
            );
            PowerHistory prev = null;
            while (cursor.moveToNext()) {
                PowerHistory row = new PowerHistory() {{
                    startTime = cursor.getLong(0);
                    endTime = startTime;
                    capacity = cursor.getInt(1);
                    screenOn = cursor.getInt(2) == 1;
                    charging = cursor.getInt(3) != BatteryManager.BATTERY_STATUS_DISCHARGING;
                }};
                if (prev == null) {
                    prev = row;
                } else if (!(row.capacity == prev.capacity && row.screenOn == prev.screenOn && row.startTime - prev.endTime < 10000)) {
                    histories.add(prev);
                    prev = row;
                } else {
                    prev.endTime = row.endTime;
                }
            }
            if (prev != null) {
                histories.add(prev);
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ignored) {
        }
        return histories;
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
