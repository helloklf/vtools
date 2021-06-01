package com.omarea.store;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.omarea.model.FpsWatchSession;

import java.util.ArrayList;

public class FpsWatchStore extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;

    public FpsWatchStore(Context context) {
        super(context, "fps_watch_log", null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("create table session(" +
                    "id INTEGER primary key, " +
                    "package_name text," +
                    "time_begin INTEGER default(-1)," +
                    "time_end INTEGER default(-1)" +
                    ")");
            db.execSQL("create table fps_history(" +
                    "id INTEGER primary key AUTOINCREMENT," +
                    "time INTEGER," +
                    "session INTEGER," +
                    "fps REAL," +
                    "temperature REAL," +
                    "power_mode text" +
                    ")");
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    // 获取总充入电量
    public int getSum() {
        int total = 0;
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            final Cursor cursor = sqLiteDatabase.rawQuery("select sum(io) as total from charge_history", new String[]{});
            while (cursor.moveToNext()) {
                total = (int) (cursor.getLong(cursor.getColumnIndex("total")) / 3600);
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ignored) {

        }
        return total;
    }

    // 列举会话
    public ArrayList<FpsWatchSession> sessions() {
        ArrayList<FpsWatchSession> histories = new ArrayList<>();
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            final Cursor cursor = sqLiteDatabase.rawQuery("select * from session", new String[]{});
            while (cursor.moveToNext()) {
                histories.add(new FpsWatchSession() {{
                    sessionId = cursor.getLong(cursor.getColumnIndex("id"));
                    packageName = cursor.getString(cursor.getColumnIndex("package_name"));
                    beginTime = cursor.getLong(cursor.getColumnIndex("time_begin"));
                }});
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ignored) {

        }
        return histories;
    }

    // 获取会话的详情（帧率）
    public ArrayList<Float> sessionFpsData(long sessionId) {
        ArrayList<Float> histories = new ArrayList<>();
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            final Cursor cursor = sqLiteDatabase.rawQuery("select fps from fps_history where session = ?", new String[]{
                    "" + sessionId
            });
            while (cursor.moveToNext()) {
                histories.add(cursor.getFloat(0));
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ignored) {

        }
        return histories;
    }
    // 获取会话的详情（温度）
    public ArrayList<Float> sessionTemperatureData(long sessionId) {
        ArrayList<Float> histories = new ArrayList<>();
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            final Cursor cursor = sqLiteDatabase.rawQuery("select temperature from fps_history where session = ?", new String[]{
                    "" + sessionId
            });
            while (cursor.moveToNext()) {
                histories.add(cursor.getFloat(0));
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ignored) {

        }
        return histories;
    }

    // 获取会话中的平静帧率
    public float sessionAvgFps(long sessionId) {
        float result = 0;
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            final Cursor cursor = sqLiteDatabase.rawQuery("select avg(fps) from fps_history where session = ?", new String[]{
                    "" + sessionId
            });
            while (cursor.moveToNext()) {
                result = cursor.getFloat(0);
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ignored) {

        }
        return result;
    }

    // 获取会话中的最低帧率
    public float sessionMinFps(long sessionId) {
        float result = 0;
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            final Cursor cursor = sqLiteDatabase.rawQuery("select min(fps) from fps_history where session = ?", new String[]{
                    "" + sessionId
            });
            while (cursor.moveToNext()) {
                result = cursor.getFloat(0);
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ignored) {

        }
        return result;
    }

    // 获取会话中的最高帧率
    public float sessionMaxFps(long sessionId) {
        float result = 0;
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            final Cursor cursor = sqLiteDatabase.rawQuery("select max(fps) from fps_history where session = ?", new String[]{
                    "" + sessionId
            });
            while (cursor.moveToNext()) {
                result = cursor.getFloat(0);
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ignored) {

        }
        return result;
    }

    // 创建会话
    public long createSession(String packageName) {
        SQLiteDatabase database = getWritableDatabase();
        getWritableDatabase().beginTransaction();
        long time = System.currentTimeMillis();
        try {
            database.execSQL("insert into session(id, package_name, time_begin) values (?, ?, ?)", new Object[]{
                    time,
                    packageName,
                    time
            });
            database.setTransactionSuccessful();
            return time;
        } catch (Exception ex) {
            return -1;
        } finally {
            database.endTransaction();
        }
    }

    // 添加记录
    public boolean addHistory(long session, float fps, float temperature, String powerMode) {
        SQLiteDatabase database = getWritableDatabase();
        getWritableDatabase().beginTransaction();
        try {
            database.execSQL("insert into fps_history(time, session, fps, temperature, power_mode) values (?, ?, ?, ?, ?)", new Object[]{
                    System.currentTimeMillis(),
                    session,
                    fps,
                    temperature,
                    powerMode
            });
            database.setTransactionSuccessful();
            return true;
        } catch (Exception ex) {
            return false;
        } finally {
            database.endTransaction();
        }
    }

    // 清空全部数据
    public boolean clearAll() {
        try {
            SQLiteDatabase database = getWritableDatabase();
            database.execSQL("delete from session", new String[]{});
            database.execSQL("delete from fps_history", new String[]{});
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    // 删除会话记录
    public boolean deleteSession(long sessionId) {
        try {
            SQLiteDatabase database = getWritableDatabase();
            database.execSQL("delete from session where id = ?", new Object[]{
                    sessionId
            });
            database.execSQL("delete from fps_history where session = ?", new Object[]{
                    sessionId
            });
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}

