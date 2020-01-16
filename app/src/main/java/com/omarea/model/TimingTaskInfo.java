package com.omarea.model;

import java.io.Serializable;
import java.util.ArrayList;

public class TimingTaskInfo implements Serializable {
    // 触发时间
    public int triggerTime = 420;
    // 重复周期（-1表示不重复）
    public int period;
    // 屏幕关闭后执行
    public boolean afterScreenOff;
    // 执行前请求确认
    public boolean beforeExecuteConfirm;
    // 电池电量要求（低于此值且未充电跳过）
    public int batteryCapacityRequire;
    // 任务动作列表
    public ArrayList<TaskAction> taskActions;
}
