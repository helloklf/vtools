package com.omarea.krscripts.action;

import com.omarea.krscripts.ConfigItem;
import java.util.ArrayList;

public class ActionInfo extends ConfigItem {
    public String descPollingShell;
    public String script;
    public String start;
    public ArrayList<ActionParamInfo> params;
}
