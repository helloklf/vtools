package com.omarea.store;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.omarea.model.SceneConfigInfo;
import com.omarea.vtools.R;

import java.util.ArrayList;

public class SceneConfigStore extends SQLiteOpenHelper {
    private static final int DB_VERSION = 6;
    private final Context context;

    public SceneConfigStore(Context context) {
        super(context, "scene3_config", null, DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(
                "create table scene_config3(" +
                    "id text primary key, " + // id
                    "alone_light int default(0), " + // 独立亮度
                    "light int default(-1), " + // 亮度
                    "dis_notice int default(0)," + // 拦截通知
                    "dis_button int default(0)," + // 停用按键
                    "gps_on int default(0)," + // 打开GPS
                    "freeze int default(0)," + // 休眠
                    "screen_orientation int default(-1)," + // 屏幕旋转方向
                    "fg_cgroup_mem text default('')," + // cgroup
                    "bg_cgroup_mem text default('')," + // cgroup
                    "dynamic_boost_mem int default(0)," + //
                    "show_monitor int default(0)" + //
                ")");

            // 初始化默认配置
            String[] gpsOnApps = this.context.getResources().getStringArray(R.array.scene_gps_on);
            for (String app: gpsOnApps) {
                db.execSQL("insert into scene_config3(id, gps_on) values (?, ?)", new Object[]{
                    app,
                    1
                });
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // 屏幕方向
            try {
                db.execSQL("alter table scene_config3 add column screen_orientation int default(-1)");
            } catch (Exception ignored) {
            }
        }

        if (oldVersion < 4) {
            try {
                db.execSQL("alter table scene_config3 add column fg_cgroup_mem text default('')");
                db.execSQL("alter table scene_config3 add column bg_cgroup_mem text default('')");
            } catch (Exception ignored) {
            }
        }

        if (oldVersion < 5) {
            try {
                db.execSQL("alter table scene_config3 add column dynamic_boost_mem text default(0)");
            } catch (Exception ignored) {
            }
        }

        if (oldVersion < 6) {
            try {
                db.execSQL("alter table scene_config3 add column show_monitor text default(0)");
            } catch (Exception ignored) {
            }
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
                sceneConfigInfo.fgCGroupMem = cursor.getString(cursor.getColumnIndex("fg_cgroup_mem"));
                sceneConfigInfo.bgCGroupMem = cursor.getString(cursor.getColumnIndex("bg_cgroup_mem"));
                sceneConfigInfo.dynamicBoostMem = cursor.getInt(cursor.getColumnIndex("dynamic_boost_mem")) == 1;
                sceneConfigInfo.showMonitor = cursor.getInt(cursor.getColumnIndex("show_monitor")) == 1;
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
            database.execSQL("delete from scene_config3 where id = ?", new String[]{sceneConfigInfo.packageName});
            database.execSQL("insert into scene_config3(id, alone_light, light, dis_notice, dis_button, gps_on, freeze, screen_orientation, fg_cgroup_mem, bg_cgroup_mem, dynamic_boost_mem, show_monitor) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{
                    sceneConfigInfo.packageName,
                    sceneConfigInfo.aloneLight ? 1 : 0,
                    sceneConfigInfo.aloneLightValue,
                    sceneConfigInfo.disNotice ? 1 : 0,
                    sceneConfigInfo.disButton ? 1 : 0,
                    sceneConfigInfo.gpsOn ? 1 : 0,
                    sceneConfigInfo.freeze ? 1 : 0,
                    sceneConfigInfo.screenOrientation,
                    sceneConfigInfo.fgCGroupMem,
                    sceneConfigInfo.bgCGroupMem,
                    sceneConfigInfo.dynamicBoostMem ? 1 : 0,
                    sceneConfigInfo.showMonitor ? 1 : 0
            });
            database.setTransactionSuccessful();
            return true;
        } catch (Exception ex) {
            return false;
        } finally {
            database.endTransaction();
        }
    }

    public boolean resetAll() {
        try {
            SQLiteDatabase database = getWritableDatabase();
            database.execSQL("update scene_config3 set alone_light = 0, fg_cgroup_mem = '', screen_orientation = ?, bg_cgroup_mem = '', dynamic_boost_mem = 0, show_monitor = 0", new Object[]{
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            });
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean removeAppConfig(String packageName) {
        try {
            SQLiteDatabase database = getWritableDatabase();
            database.execSQL("delete from scene_config3 where id = ?", new String[]{packageName});
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
}
