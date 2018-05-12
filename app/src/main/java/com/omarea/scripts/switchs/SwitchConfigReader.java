package com.omarea.scripts.switchs;

import android.content.Context;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import com.omarea.scripts.ExtractAssets;
import com.omarea.scripts.simple.shell.ExecuteCommandWithOutput;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by Hello on 2018/04/01.
 */

public class SwitchConfigReader {
    private static final String ASSETS_FILE = "file:///android_asset/";

    private static InputStream getConfig(Context context){
        try {
            return context.getAssets().open("switchs.xml");
        } catch (Exception ex) {
            return null;
        }
    }

    public static ArrayList<SwitchInfo> readActionConfigXml(Context context) {
        try {
            InputStream fileInputStream = getConfig(context);
            if (fileInputStream == null)
                return new ArrayList<>();
            XmlPullParser parser = Xml.newPullParser();// 获取xml解析器
            parser.setInput(fileInputStream, "utf-8");// 参数分别为输入流和字符编码
            int type = parser.getEventType();
            ArrayList<SwitchInfo> actions = null;
            SwitchInfo action = null;
            while (type != XmlPullParser.END_DOCUMENT) {// 如果事件不等于文档结束事件就继续循环
                switch (type) {
                    case XmlPullParser.START_TAG:
                        if ("switchs".equals(parser.getName())) {
                            actions = new ArrayList<>();
                        } else if ("switch".equals(parser.getName())) {
                            action = new SwitchInfo();
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                switch (parser.getAttributeName(i)) {
                                    case "root": {
                                        action.root = Objects.equals(parser.getAttributeValue(i), "true");
                                        break;
                                    }
                                    case "confirm": {
                                        action.confirm = Objects.equals(parser.getAttributeValue(i), "true");
                                        break;
                                    }
                                    case "start": {
                                        action.start = parser.getAttributeValue(i);
                                        break;
                                    }
                                }
                            }
                        } else if (action != null) {
                            if ("title".equals(parser.getName())) {
                                action.title = parser.nextText();
                            } else if ("desc".equals(parser.getName())) {
                                for (int i = 0; i < parser.getAttributeCount(); i++) {
                                    String attrValue = parser.getAttributeValue(i);
                                    switch (parser.getAttributeName(i)) {
                                        case "su": {
                                            if (attrValue.trim().startsWith(ASSETS_FILE)) {
                                                String path = new ExtractAssets(context).extractToFilesDir(attrValue.trim());
                                                action.descPollingSUShell = "chmod 7777 " + path + "\n" + path;
                                            } else {
                                                action.descPollingSUShell = attrValue;
                                            }
                                            action.desc = executeResultRoot(context, action.descPollingSUShell);
                                            break;
                                        }
                                        case "sh": {
                                            if (attrValue.trim().startsWith(ASSETS_FILE)) {
                                                String path = new ExtractAssets(context).extractToFilesDir(attrValue.trim());
                                                action.descPollingShell = "chmod 7777 " + path + "\n" + path;
                                            } else {
                                                action.descPollingShell = attrValue;
                                            }
                                            action.desc = executeResultRoot(context, action.descPollingShell);
                                            break;
                                        }
                                    }
                                }
                                if (action.desc == null || action.desc.isEmpty())
                                    action.desc = parser.nextText();
                            } else if ("getstate".equals(parser.getName())) {
                                String script = parser.nextText();
                                if (script.trim().startsWith(ASSETS_FILE)) {
                                    action.getStateType = SwitchInfo.ActionScript.ASSETS_FILE;
                                    String path = new ExtractAssets(context).extractToFilesDir(script.trim());
                                    action.getState = "chmod 7777 " + path + "\n" + path;
                                } else {
                                    action.getState = script;
                                }
                            } else if ("setstate".equals(parser.getName())) {
                                String script = parser.nextText();
                                if (script.trim().startsWith(ASSETS_FILE)) {
                                    action.setStateType = SwitchInfo.ActionScript.ASSETS_FILE;
                                    String path = new ExtractAssets(context).extractToFilesDir(script.trim());
                                    action.setState = "chmod 7777 " + path + "\n" + path;
                                } else {
                                    action.setState = script;
                                }
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if ("switch".equals(parser.getName()) && actions != null && action != null) {
                            if (action.title == null) {
                                action.title = "";
                            }
                            if (action.desc == null) {
                                action.desc = "";
                            }
                            if (action.getState == null) {
                                action.getState = "";
                            } else {
                                String shellResult = ExecuteCommandWithOutput.executeCommandWithOutput(action.root, action.getState);
                                action.selected = shellResult != null && (shellResult.equals("1") || shellResult.toLowerCase().equals("true"));
                            }
                            if (action.setState == null) {
                                action.setState = "";
                            }

                            actions.add(action);
                            action = null;
                        }
                        break;
                }
                type = parser.next();// 继续下一个事件
            }

            return actions;
        } catch (Exception ex) {
            Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
            Log.d("VTools ReadConfig Fail！", ex.getMessage());
        }
        return null;
    }

    private static String executeResult(Context context, String script) {
        if (script.trim().startsWith(ASSETS_FILE)) {
            String path = new ExtractAssets(context).extractToFilesDir(script.trim());
            script = "chmod 7777 " + path + "\n" + path;
        }
        String shellResult = ExecuteCommandWithOutput.executeCommandWithOutput(false, script);
        return shellResult;
    }

    private static String executeResultRoot(Context context, String script) {
        if (script.trim().startsWith(ASSETS_FILE)) {
            String path = new ExtractAssets(context).extractToFilesDir(script.trim());
            script = "chmod 7777 " + path + "\n" + path;
        }
        String shellResult = ExecuteCommandWithOutput.executeCommandWithOutput(true, script);
        return shellResult;
    }
}
