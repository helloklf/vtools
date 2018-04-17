package com.omarea.scripts.action;

import android.content.Context;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;
import com.omarea.scripts.ExtractAssets;
import org.xmlpull.v1.XmlPullParser;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by Hello on 2018/04/01.
 */

public class ActionConfigReader {
    private static final String ASSETS_FILE = "file:///android_asset/";

    public static ArrayList<ActionInfo> readActionConfigXml(Context context) {
        try {
            InputStream fileInputStream = context.getAssets().open("actions.xml");
            XmlPullParser parser = Xml.newPullParser();// 获取xml解析器
            parser.setInput(fileInputStream, "utf-8");// 参数分别为输入流和字符编码
            int type = parser.getEventType();
            ArrayList<ActionInfo> actions = null;
            ActionInfo action = null;
            ArrayList<ActionParamInfo> actionParamInfos = null;
            ActionParamInfo actionParamInfo = null;
            while (type != XmlPullParser.END_DOCUMENT) {// 如果事件不等于文档结束事件就继续循环
                switch (type) {
                    case XmlPullParser.START_TAG:
                        if ("actions".equals(parser.getName())) {
                            actions = new ArrayList<>();
                        } else if ("action".equals(parser.getName())) {
                            action = new ActionInfo();
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
                                action.desc = parser.nextText();
                            } else if ("script".equals(parser.getName())) {
                                String script = parser.nextText();
                                if (script.trim().startsWith(ASSETS_FILE)) {
                                    action.scriptType = ActionInfo.ActionScript.ASSETS_FILE;
                                    String path = new ExtractAssets(context).extractToFilesDir(script.trim());
                                    action.script = "chmod 7777 " + path + "\n" + path;
                                } else {
                                    action.script = script;
                                }
                            } else if ("param".equals(parser.getName())) {
                                if (actionParamInfos == null) {
                                    actionParamInfos = new ArrayList<>();
                                }
                                actionParamInfo = new ActionParamInfo();
                                //actionParamInfo.desc = parser.nextText();
                                for (int i = 0; i < parser.getAttributeCount(); i++) {
                                    switch (parser.getAttributeName(i)) {
                                        case "name": {
                                            actionParamInfo.name = parser.getAttributeValue(i);
                                            break;
                                        }
                                        case "desc": {
                                            actionParamInfo.desc = parser.getAttributeValue(i);
                                            break;
                                        }
                                        case "value": {
                                            actionParamInfo.value = parser.getAttributeValue(i);
                                            break;
                                        }
                                        case "type": {
                                            actionParamInfo.type = parser.getAttributeValue(i).toLowerCase().trim();
                                            break;
                                        }
                                        case "readonly": {
                                            actionParamInfo.readonly = parser.getAttributeValue(i).toLowerCase().trim().equals("readonly");
                                            break;
                                        }
                                        case "maxlength": {
                                            actionParamInfo.maxLength = Integer.parseInt(parser.getAttributeValue(i));
                                            break;
                                        }
                                        case "maxLength": {
                                            actionParamInfo.maxLength = Integer.parseInt(parser.getAttributeValue(i));
                                            break;
                                        }
                                    }
                                }
                                if (actionParamInfo.name != null && !actionParamInfo.name.trim().equals("")) {
                                    actionParamInfos.add(actionParamInfo);
                                }
                            } else if (actionParamInfo != null && "option".equals(parser.getName())) {
                                if (actionParamInfo.options == null) {
                                    actionParamInfo.options = new ArrayList<>();
                                }
                                ActionParamInfo.ActionParamOption option = new ActionParamInfo.ActionParamOption();
                                for (int i = 0; i < parser.getAttributeCount(); i++) {
                                    switch (parser.getAttributeName(i)) {
                                        case "value": {
                                            option.value = parser.getAttributeValue(i);
                                            break;
                                        }
                                        case "val": {
                                            option.value = parser.getAttributeValue(i);
                                            break;
                                        }
                                    }
                                }
                                option.desc = parser.nextText();
                                if (option.value == null)
                                    option.value = option.desc;
                                actionParamInfo.options.add(option);
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if ("action".equals(parser.getName()) && actions != null && action != null) {
                            if (action.title == null) {
                                action.title = "";
                            }
                            if (action.desc == null) {
                                action.desc = "";
                            }
                            if (action.script == null) {
                                action.script = "";
                            }
                            action.params = actionParamInfos;

                            actions.add(action);
                            actionParamInfos = null;
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
}
