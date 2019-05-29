package com.omarea.krscript.model;

import com.omarea.krscript.config.ActionParamInfo;

import java.util.ArrayList;

public class ActionInfo extends ConfigItemBase {
    public String descPollingShell;
    public String script;
    public String start;
    public ArrayList<ActionParamInfo> params;
}
