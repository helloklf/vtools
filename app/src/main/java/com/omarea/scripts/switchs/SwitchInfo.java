package com.omarea.scripts.switchs;

public class SwitchInfo {
    public String title;
    public String desc;
    public String descPollingSUShell;
    public String descPollingShell;
    public String getState;
    public String setState;
    public boolean selected;
    public String start;
    public ActionScript getStateType = ActionScript.SCRIPT;
    public ActionScript setStateType = ActionScript.SCRIPT;
    public boolean root;
    public boolean confirm;

    public enum ActionScript {
        SCRIPT,
        ASSETS_FILE
    }
}
