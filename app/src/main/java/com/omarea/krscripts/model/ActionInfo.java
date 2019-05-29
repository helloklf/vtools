package com.omarea.krscripts.model;

import com.omarea.krscripts.config.ActionParamInfo;

import java.util.ArrayList;

public class ActionInfo extends ConfigItemBase {
    public String descPollingShell;
    public String script;
    public String start;
    public ArrayList<ActionParamInfo> params;
}
