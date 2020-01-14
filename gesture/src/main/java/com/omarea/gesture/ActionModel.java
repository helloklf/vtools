package com.omarea.gesture;

import android.content.SharedPreferences;

import java.io.Serializable;

public class ActionModel implements Serializable {
    public int actionCode;
    public String title;
    public String exKey;
    public String st;
    public String shellCommand;

    private ActionModel(int code) {
        this.actionCode = code;
    }

    public ActionModel(int code, String title) {
        this.actionCode = code;
        this.title = title;
    }

    public static ActionModel getConfig(SharedPreferences config, String configKey, int defaultAction) {
        ActionModel actionModel = new ActionModel(config.getInt(configKey, defaultAction));
        actionModel.exKey = configKey;

        return actionModel;
    }
}
