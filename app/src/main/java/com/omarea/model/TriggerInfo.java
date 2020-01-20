package com.omarea.model;

import com.omarea.data_collection.EventType;

import java.io.Serializable;
import java.util.ArrayList;

public class TriggerInfo implements Serializable {
    public TriggerInfo(String id) {
        this.id = id;
    }

    // 是否启用
    public boolean enabled;

    // id
    public String id;

    // 事件
    public ArrayList<EventType> events;

    // 任务动作列表
    public ArrayList<TaskAction> taskActions;
}
