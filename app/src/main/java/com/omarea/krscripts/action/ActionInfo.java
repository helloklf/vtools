package com.omarea.krscripts.action;

import com.omarea.krscripts.ConfigItem;
import java.util.ArrayList;

public class ActionInfo extends ConfigItem {
    public String separator;
    public String title;
    public String desc;
    public String descPollingShell;
    public String script;
    public String start;
    public ArrayList<ActionParamInfo> params;
    public boolean confirm;
}
