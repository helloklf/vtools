package com.omarea.shared;

import com.omarea.shell.Platform;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Hello on 2017/4/8.
 */

public class ConfigInfo {
    private static ConfigInfo configInfo;

    public boolean AutoStartSwap = false;
    public boolean AutoStartSwapDisZram = false;
    public int AutoStartSwappiness = 65;
    public int AutoStartZRAMSize = 0;
    public boolean AutoStartZRAM = false;
    public boolean AutoBooster = true;
    public boolean DyamicCore = false;
    public boolean QcMode = true;
    public boolean UseBigCore = false;
    public boolean AutoInstall = true;
    public boolean DebugMode = false;
    public boolean DelayStart = false;
    //public boolean PowerAdapter = true;
    public boolean HasSystemApp = false;
    public boolean BatteryProtection = false;
    public int BatteryProtectionLevel = 85;
    public boolean AutoClearCache = false;
    public boolean UsingDozeMod = false;
    public String CPUName = null;

    public ArrayList<HashMap<String, Object>> defaultList;
    public ArrayList<HashMap<String, Object>> gameList;
    public ArrayList<HashMap<String, Object>> powersaveList;
    public ArrayList<HashMap<String, Object>> fastList;
    public ArrayList<HashMap<String, Object>> ignoredList;
    public ArrayList<String> blacklist;


    private ConfigInfo(JSONObject jsonObject) {
        this.AutoStartSwap = isTrue(jsonObject,"AutoStartSwap",false);
        this.AutoStartSwapDisZram = isTrue(jsonObject,"AutoStartSwapDisZram",false);
        this.AutoStartSwappiness = getInt(jsonObject,"AutoStartSwappiness",65);
        this.AutoStartZRAMSize = getInt(jsonObject,"AutoStartZRAMSize",0);
        this.AutoStartZRAM = isTrue(jsonObject,"AutoStartZRAM",false);
        this.AutoInstall = isTrue(jsonObject, "AutoInstall", true);
        this.AutoBooster = isTrue(jsonObject, "AutoBooster", true);
        this.DyamicCore = isTrue(jsonObject, "DyamicCore");
        this.DebugMode = isTrue(jsonObject, "DebugMode");
        this.DelayStart = isTrue(jsonObject, "DelayStart");
        this.QcMode = isTrue(jsonObject, "QcMode", true);
        this.UseBigCore = isTrue(jsonObject, "UseBigCore");
        this.BatteryProtection = isTrue(jsonObject, "BatteryProtection");
        this.HasSystemApp = isTrue(jsonObject, "HasSystemApp");
        this.AutoClearCache = isTrue(jsonObject, "AutoClearCache");
        this.UsingDozeMod = isTrue(jsonObject, "UsingDozeMod");

        this.defaultList = new ArrayList<>();
        this.gameList = getHashMapList(jsonObject,new String[]{"name", "packageName"}, "Profile_Games");
        this.powersaveList = getHashMapList(jsonObject,new String[]{"name", "packageName"}, "Profile_PowerSave");
        this.fastList = getHashMapList(jsonObject,new String[]{"name", "packageName"}, "Profile_Fast");
        this.ignoredList = getHashMapList(jsonObject,new String[]{"name", "packageName"}, "Profile_Ignored");
        this.blacklist = getArray(jsonObject,"Booster_BlackList");
        this.BatteryProtectionLevel = getInt(jsonObject, "BatteryProtectionLevel", 85);
    }

    /**
     * 获取配置信息
     *
     * @return
     */
    public static ConfigInfo getConfigInfo() {
        if (configInfo == null) {
            JSONObject jsonObject = AppShared.INSTANCE.getConfigData();
            configInfo = new ConfigInfo(jsonObject);
            configInfo.CPUName = new Platform().GetCPUName();
        }

        return configInfo;
    }


    public boolean saveChange() {
        try {
            JSONObject jsonObject = new JSONObject()
                    .put("UsingDozeMod", UsingDozeMod)
                    .put("AutoStartSwap",AutoStartSwap)
                    .put("AutoStartSwapDisZram",AutoStartSwapDisZram)
                    .put("AutoStartSwappiness",AutoStartSwappiness)
                    .put("AutoStartZRAM",AutoStartZRAM)
                    .put("AutoStartZRAMSize",AutoStartZRAMSize)
                    .put("AutoBooster", AutoBooster)
                    .put("DyamicCore", DyamicCore)
                    .put("QcMode", QcMode)
                    .put("UseBigCore", UseBigCore)
                    .put("AutoInstall", AutoInstall)
                    .put("DebugMode", DebugMode)
                    .put("DelayStart", DelayStart)
                    .put("HasSystemApp", HasSystemApp)
                    .put("AutoClearCache",AutoClearCache)
                    .put("BatteryProtection", BatteryProtection)
                    .put("BatteryProtectionLevel", BatteryProtectionLevel);

            setHashMapList(jsonObject,gameList,"Profile_Games");
            setHashMapList(jsonObject,powersaveList,"Profile_PowerSave");
            setHashMapList(jsonObject,fastList,"Profile_Fast");
            setHashMapList(jsonObject,ignoredList,"Profile_Ignored");
            setArray(jsonObject,blacklist,"Booster_BlackList");

            return AppShared.INSTANCE.setConfigData(jsonObject);
        } catch (Exception ex) {
            return false;
        }
    }

    private int getInt(JSONObject jsonObject, String propName,int defaultVal){
        try{
            if (propName == null || !jsonObject.has(propName))
                return defaultVal;
            else
                return jsonObject.getInt(propName);
        }
        catch (Exception ex){
            return defaultVal;
        }
    }

    private boolean isTrue(JSONObject jsonObject, String propName) {
        if (propName == null || !jsonObject.has(propName))
            return false;
        try {
            return jsonObject.getBoolean(propName);
        } catch (JSONException e) {
            return false;
        }
    }

    private boolean isTrue(JSONObject jsonObject, String propName, Boolean def) {
        if (propName == null || !jsonObject.has(propName))
            return def;
        try {
            return jsonObject.getBoolean(propName);
        } catch (JSONException e) {
            return def;
        }
    }

    public static ArrayList<HashMap<String, Object>> getHashMapList(JSONObject jsonObject, String[] items, String prop) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
        try {
            JSONArray gameList = jsonObject.getJSONArray(prop);

            JSONObject row;
            String val;
            for (int i = 0; i < gameList.length(); i++) {
                row = new JSONObject(gameList.get(i).toString());
                HashMap<String, Object> rowDic = new HashMap<String, Object>();

                for (String key : items) {
                    val = row.getString(key);
                    if (val != null)
                        val = URLDecoder.decode(val, "UTF-8");
                    rowDic.put(key, val);
                }
                list.add(rowDic);
            }
        } catch (Exception ex) {
        }
        return list;
    }

    public static void setHashMapList(JSONObject jsonObject,List<HashMap<String, Object>> map, String prop) {
        Object obj;
        HashMap<String, Object> row;
        List<HashMap<String, Object>> newDic = new ArrayList<>();
        try {
            for (HashMap<String, Object> item : map) {
                row = new HashMap<String, Object>();
                for (String key : item.keySet()) {
                    obj = item.get(key);
                    if (obj != null) {
                        row.put(key, URLEncoder.encode(obj.toString(), "UTF-8"));
                    } else {
                        row.put(key, obj);
                    }
                }
                newDic.add(row);
            }
            jsonObject.put(prop, new JSONArray(newDic));
        } catch (Exception ex) {

        }
    }


    public static ArrayList<String> getArray(JSONObject jsonObject,String prop) {
        ArrayList<String> list = new ArrayList<>();
        try {
            JSONArray jsonArray = jsonObject.getJSONArray(prop);
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.getString(i));
            }
        } catch (Exception ex) {

        }
        return list;
    }

    public static void setArray(JSONObject jsonObject, ArrayList<String> datas,String prop) {
        try {
            jsonObject.put(prop, new JSONArray(datas));
        } catch (Exception ex) {

        }
    }
}
