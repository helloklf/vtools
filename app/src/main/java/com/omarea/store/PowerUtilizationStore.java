package com.omarea.store;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.omarea.model.ChargeSpeedHistory;
import com.omarea.model.PowerHistory;

import java.util.ArrayList;

public class PowerUtilizationStore extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;

    public PowerUtilizationStore(Context context) {
        super(context, "power_utilization3", null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("create table power_utilization(" +
                "id INTEGER primary key AUTOINCREMENT, " +
                "time INTEGER, " +
                "io INTEGER, " +
                "capacity INTEGER, " +
                "temperature REAL, " +
                "screen_on INTEGER" +
            ")");
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public ArrayList<PowerHistory> chargeTime() {
        ArrayList<PowerHistory> histories = new ArrayList<>();
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            final Cursor cursor = sqLiteDatabase.rawQuery(
            "select capacity, min(time) as start_time, max(time) as end_time, screen_on from power_utilization group by capacity,screen_on order by capacity DESC,start_time",
                new String[]{}
            );
            while (cursor.moveToNext()) {
                histories.add(new PowerHistory() {{
                    startTime = cursor.getLong(cursor.getColumnIndex("start_time"));
                    endTime = cursor.getLong(cursor.getColumnIndex("end_time"));
                    capacity = cursor.getInt(cursor.getColumnIndex("capacity"));
                    screenOn = cursor.getInt(cursor.getColumnIndex("screen_on")) == 1;
                }});
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ex) {
            ex.getMessage();
        }
        return histories;
    }

    public boolean addHistory(long io, int capacity, double temperature, boolean screenOn) {
        SQLiteDatabase database = getWritableDatabase();
        getWritableDatabase().beginTransaction();
        try {
            database.execSQL("insert into power_utilization(time, io, capacity, temperature, screen_on) values (?, ?, ?, ?, ?)", new Object[]{
                System.currentTimeMillis(),
                io,
                capacity,
                temperature,
                screenOn ? 1 : 0
            });
            database.setTransactionSuccessful();
            return true;
        } catch (Exception ex) {
            return false;
        } finally {
            database.endTransaction();
        }
    }

    public int lastCapacity() {
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            final Cursor cursor = sqLiteDatabase.rawQuery("select max(capacity) AS capacity from power_utilization", new String[]{});
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

    public boolean clearAll() {
        try {
            SQLiteDatabase database = getWritableDatabase();
            database.execSQL("delete from power_utilization", new String[]{});
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}

