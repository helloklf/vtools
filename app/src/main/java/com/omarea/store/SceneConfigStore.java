package com.omarea.store;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.omarea.model.SceneConfigInfo;

import java.util.ArrayList;

public class SceneConfigStore extends SQLiteOpenHelper {
    private static final int DB_VERSION = 2;

    public SceneConfigStore(Context context) {
        super(context, "scene3_config", null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("create table scene_config3(" +
                    "id text primary key, " + // id
                    "alone_light int default(0), " + // 独立亮度
                    "light int default(-1), " + // 亮度
                    "dis_notice int default(0)," + // 拦截通知
                    "dis_button int default(0)," + // 停用按键
                    "gps_on int default(0)," + // 打开GPS
                    "freeze int default(0)" + // 休眠
                    ")");
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // 屏幕方向
            db.execSQL("alter table scene_config3 add column screen_orientation int default(-1)");
        }
    }

    public SceneConfigInfo getAppConfig(String app) {
        SceneConfigInfo sceneConfigInfo = new SceneConfigInfo();
        sceneConfigInfo.packageName = app;
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("select * from scene_config3 where id = ?", new String[]{app});
            if (cursor.moveToNext()) {
                sceneConfigInfo.aloneLight = cursor.getInt(cursor.getColumnIndex("alone_light")) == 1;
                sceneConfigInfo.aloneLightValue = cursor.getInt(cursor.getColumnIndex("light"));
                sceneConfigInfo.disNotice = cursor.getInt(cursor.getColumnIndex("dis_notice")) == 1;
                sceneConfigInfo.disButton = cursor.getInt(cursor.getColumnIndex("dis_button")) == 1;
                sceneConfigInfo.gpsOn = cursor.getInt(cursor.getColumnIndex("gps_on")) == 1;
                sceneConfigInfo.freeze = cursor.getInt(cursor.getColumnIndex("freeze")) == 1;
                sceneConfigInfo.screenOrientation = cursor.getInt(cursor.getColumnIndex("screen_orientation"));
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ignored) {

        }
        return sceneConfigInfo;
    }

    public boolean setAppConfig(SceneConfigInfo sceneConfigInfo) {
        SQLiteDatabase database = getWritableDatabase();
        getWritableDatabase().beginTransaction();
        try {
            database.execSQL("delete from  scene_config3 where id = ?", new String[]{sceneConfigInfo.packageName});
            database.execSQL("insert into scene_config3(id, alone_light, light, dis_notice, dis_button, gps_on, freeze, screen_orientation) values (?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{
                    sceneConfigInfo.packageName,
                    sceneConfigInfo.aloneLight ? 1 : 0,
                    sceneConfigInfo.aloneLightValue,
                    sceneConfigInfo.disNotice ? 1 : 0,
                    sceneConfigInfo.disButton ? 1 : 0,
                    sceneConfigInfo.gpsOn ? 1 : 0,
                    sceneConfigInfo.freeze ? 1 : 0,
                    sceneConfigInfo.screenOrientation
            });
            database.setTransactionSuccessful();
            return true;
        } catch (Exception ex) {
            return false;
        } finally {
            database.endTransaction();
        }
    }


    public boolean removeAppConfig(String packageName) {
        try {
            SQLiteDatabase database = getWritableDatabase();
            database.execSQL("delete from  scene_config3 where id = ?", new String[]{packageName});
            return true;
        } catch (Exception ex) {
            return false;
        }
    }


    public ArrayList<String> getFreezeAppList() {
        ArrayList<String> list = new ArrayList<String>();
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("select * from scene_config3 where freeze == 1", null);
            while (cursor.moveToNext()) {
                list.add(cursor.getString(0));
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ignored) {
        }
        return list;
    }

    public boolean needKeyCapture() {
        boolean r = false;
        try {
            SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("select * from scene_config3 where dis_button == 1", new String[]{});
            cursor.moveToFirst();
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
