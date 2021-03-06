package com.omarea.model;

import android.content.pm.ActivityInfo;

public class SceneConfigInfo {
    public String packageName;

    // 使用独立亮度
    public boolean aloneLight = false;
    // 独立亮度值
    public int aloneLightValue = -1;
    // 屏蔽通知
    public boolean disNotice = false;
    // 拦截按键
    public boolean disButton = false;
    // 启动时开启GPS
    public boolean gpsOn = false;
    // 应用偏见（自动冻结）
    public boolean freeze = false;
    // 屏幕旋转方向
    public int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

    // cgroup - memory
    public String fgCGroupMem = "";
    public String bgCGroupMem = "";
    public boolean dynamicBoostMem = false;

    // 显示性能监视器
    public boolean showMonitor = false;
}
