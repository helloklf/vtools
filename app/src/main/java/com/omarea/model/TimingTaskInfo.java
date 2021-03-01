package com.omarea.model;

import java.io.Serializable;
import java.util.ArrayList;

public class TimingTaskInfo implements Serializable {
    // 任务id
    public String taskId;
    // 任务名称
    public String taskName;
    // 是否启用
    public boolean enabled;
    // 触发时间 hours * 60 + minutes， 例如 6:30 表示为 6 * 60 + 30 = 390
    public int triggerTimeMinutes = 420;
    // 任务失效时间
    public long expireDate;
    // 屏幕关闭后执行
    public boolean afterScreenOff;
    // 执行前请求确认
    public boolean beforeExecuteConfirm;
    // 电池电量要求（低于此值且未充电跳过）
    public int batteryCapacityRequire;
    // 是否只在充电状态下才执行
    public boolean chargeOnly;
    // 任务动作列表
    public ArrayList<TaskAction> taskActions;
    // 任务动作列表（自定义）
    public ArrayList<CustomTaskAction> customTaskActions;

    public TimingTaskInfo() {
    }

    public TimingTaskInfo(String taskId) {
        this.taskId = taskId;
    }
}
