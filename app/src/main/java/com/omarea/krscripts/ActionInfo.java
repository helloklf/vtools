package com.omarea.krscripts;

import com.omarea.krscripts.ConfigItem;
import com.omarea.krscripts.action.ActionParamInfo;

import java.util.ArrayList;

public class ActionInfo extends ConfigItem {
    public String descPollingShell;
    public String script;
    public String start;
    public ArrayList<ActionParamInfo> params;
}
