package com.omarea.data_collection;

public enum EventType {
    POWER_CONNECTED,        // 充电器连接
    POWER_DISCONNECTED,     // 充电器连接
    CHARGER_DISCONNECTED,   // 充电器断开连接
    BATTERY_LOW,            // 电池电量不足
    BATTERY_CHANGED,        // 电量改变
    SCREEN_ON,              // 屏幕打开
    SCREEN_OFF,             // 屏幕关闭
    APP_SWITCH;             // 应用切换
}
