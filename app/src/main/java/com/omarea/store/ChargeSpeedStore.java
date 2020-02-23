package com.omarea.store;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.omarea.model.ChargeSpeedHistory;

import java.util.ArrayList;

public class ChargeSpeedStore extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;

    public ChargeSpeedStore(Context context) {
        super(context, "charge_history", null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("create table charge_history(" +
                    "id INTEGER primary key AUTOINCREMENT, " +
                    "time INTEGER, " +
                    "io INTEGER, " +
                    "capacity INTEGER " +
                    ")");
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public ArrayList<ChargeSpeedHistory> statistics() {
        ArrayList<ChargeSpeedHistory> histories = new ArrayList<>();
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            final Cursor cursor = sqLiteDatabase.rawQuery("select capacity, avg(io) as io from charge_history group by capacity", new String[]{});
            while (cursor.moveToNext()) {
                histories.add(new ChargeSpeedHistory(){{
                    capacity = cursor.getInt(cursor.getColumnIndex("capacity"));
                    io = cursor.getLong(cursor.getColumnIndex("io"));
                }});
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ignored) {

        }
        return histories;
    }

    public boolean addHistory(long io, int capacity) {
        SQLiteDatabase database = getWritableDatabase();
        getWritableDatabase().beginTransaction();
        try {
            database.execSQL("insert into charge_history(time, io, capacity) values (?, ?, ?)", new Object[]{
                    System.currentTimeMillis(),
                    io,
                    capacity
            });
            database.setTransactionSuccessful();
            return true;
        } catch (Exception ex) {
            return false;
        } finally {
            database.endTransaction();
        }
    }

    public boolean clearAll() {
        try {
            SQLiteDatabase database = getWritableDatabase();
            database.execSQL("delete from  charge_history", new String[]{});
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
