package com.omarea.scripts.action;

import java.util.ArrayList;

public class ActionInfo {
    public String title;
    public String desc;
    public String descPollingSUShell;
    public String descPollingShell;
    //FIXME:暂时没用
    public int polling = 0;
    public String script;
    public String start;
    public ActionScript scriptType = ActionScript.SCRIPT;
    public ArrayList<ActionParamInfo> params;

    public boolean root;
    public boolean confirm;

    public enum ActionScript {
        SCRIPT,
        ASSETS_FILE
    }
}
