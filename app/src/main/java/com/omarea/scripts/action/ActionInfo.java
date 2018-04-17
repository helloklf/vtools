package com.omarea.scripts.action;

import java.util.ArrayList;

public class ActionInfo {
    String title;
    String desc;
    String script;
    String start;
    ActionScript scriptType = ActionScript.SCRIPT;
    ArrayList<ActionParamInfo> params;

    boolean root;
    boolean confirm;

    enum  ActionScript {
        SCRIPT,
        ASSETS_FILE
    }
}
