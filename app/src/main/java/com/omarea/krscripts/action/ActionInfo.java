package com.omarea.krscripts.action;

import java.util.ArrayList;

public class ActionInfo {
    public String separator;
    public String title;
    public String desc;
    public String descPollingShell;
    public String script;
    public String start;
    public ActionScript scriptType = ActionScript.SCRIPT;
    public ArrayList<ActionParamInfo> params;
    public boolean confirm;

    public enum ActionScript {
        SCRIPT,
        ASSETS_FILE
    }
}
