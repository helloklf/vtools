package com.omarea.data;

public enum EventType {
    POWER_CONNECTED,            // 充电器连接
    POWER_DISCONNECTED,         // 充电器连接
    BATTERY_LOW,                // 电池电量不足
    BATTERY_CAPACITY_CHANGED,   // 电池电量变化
    BATTERY_CHANGED,            // 电池状态变化
    BATTERY_FULL,               // 电池充满
    CHARGE_CONFIG_CHANGED,      // 充电控制配置改变
    SCREEN_ON,                  // 屏幕打开
    SCREEN_OFF,                 // 屏幕关闭
    APP_SWITCH,                 // 应用切换
    BOOT_COMPLETED,             // 启动完成
    TIMER,                      // 定时器

    SERVICE_DEBUG,             // 服务调试配置更新
    SERVICE_UPDATE,             // 服务配置更新
    STATE_RESUME,               // 状态恢复（一般指屏幕点亮后应用场景模式配置）

    SCENE_MODE_ACTION,          // 场景模式 常驻通知动作触发
    SCENE_CONFIG,               // 场景模式 公共配置
    SCENE_APP_CONFIG,           // 场景模式 对应用的配置
}
