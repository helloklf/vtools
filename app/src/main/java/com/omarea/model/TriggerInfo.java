package com.omarea.model;

import com.omarea.data.EventType;

import java.io.Serializable;
import java.util.ArrayList;

public class TriggerInfo implements Serializable {
    // 是否启用
    public boolean enabled;
    // id
    public String id;
    // 事件
    public ArrayList<EventType> events;

    // 是否限制执行时间段
    public boolean timeLimited = false;
    // 时间段 - 开始时间
    public int timeStart = 0;
    // 时间段 - 结束时间
    public int timeEnd = 24 * 60 - 1;

    // 任务动作列表
    public ArrayList<TaskAction> taskActions;
    // 任务动作列表（自定义）
    public ArrayList<CustomTaskAction> customTaskActions;

    public TriggerInfo(String id) {
        this.id = id;
    }
}
