package com.omarea.scripts.switchs;

import android.content.Context;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import com.omarea.scripts.ExtractAssets;

import org.xmlpull.v1.XmlPullParser;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by Hello on 2018/04/01.
 */

public class SwitchConfigReader {
    private static final String ASSETS_FILE = "file:///android_asset/";

    public static ArrayList<ActionInfo> readActionConfigXml(Context context) {
        try {
            InputStream fileInputStream = context.getAssets().open("switchs.xml");
            XmlPullParser parser = Xml.newPullParser();// 获取xml解析器
            parser.setInput(fileInputStream, "utf-8");// 参数分别为输入流和字符编码
            int type = parser.getEventType();
            ArrayList<ActionInfo> actions = null;
            ActionInfo action = null;
            while (type != XmlPullParser.END_DOCUMENT) {// 如果事件不等于文档结束事件就继续循环
                switch (type) {
                    case XmlPullParser.START_TAG:
                        if ("switchs".equals(parser.getName())) {
                            actions = new ArrayList<>();
                        } else if ("switch".equals(parser.getName())) {
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
                            } else if ("getstate".equals(parser.getName())) {
                                String script = parser.nextText();
                                if (script.trim().startsWith(ASSETS_FILE)) {
                                    action.getStateType = ActionInfo.ActionScript.ASSETS_FILE;
                                    String path = new ExtractAssets(context).extractToFilesDir(script.trim());
                                    action.getState = "chmod 7777 " + path + "\n" + path;
                                } else {
                                    action.getState = script;
                                }
                            } else if ("setstate".equals(parser.getName())) {
                                String script = parser.nextText();
                                if (script.trim().startsWith(ASSETS_FILE)) {
                                    action.setStateType = ActionInfo.ActionScript.ASSETS_FILE;
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
                                action.selected = executeCommandWithOutput(action.root, action.getState).equals("1");
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


    public static String executeCommandWithOutput(boolean root, String command) {
        DataOutputStream dos;
        InputStream is;
        try {
            Process process;
            process = root ? Runtime.getRuntime().exec("su") : Runtime.getRuntime().exec("sh");
            if (process == null) return "";
            dos = new DataOutputStream(process.getOutputStream());
            dos.writeBytes(command + "\n");
            dos.writeBytes("exit \n");
            dos.writeBytes("exit \n");
            dos.flush();
            dos.close();
            if (process.waitFor() == 0) {
                is = process.getInputStream();
                StringBuilder builder = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = br.readLine()) != null)
                    builder.append(line.trim()).append("\n");
                return builder.toString().trim();
            } else {
                is = process.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = br.readLine()) != null) Log.d("error", line);
            }
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        return "";
    }
}
