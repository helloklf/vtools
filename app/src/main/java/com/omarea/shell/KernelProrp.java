package com.omarea.shell;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 操作内核参数节点
 * Created by Hello on 2017/11/01.
 */

public class KernelProrp {
    /**
     * 获取属性
     *
     * @param propName 属性名称
     * @return
     */
    public static String getProp(String propName) {
        return getProp(propName, false);
    }
    /**
     * 获取属性
     *
     * @param propName 属性名称
     * @return
     */
    public static String getProp(String propName, Boolean root) {
        if (!new File(propName).exists()) {
            return "";
        }
        try {
            Process p = Runtime.getRuntime().exec(root ? "su" : "sh");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.write(("cat " + propName).getBytes("UTF-8"));
            out.writeBytes("\n");
            out.writeBytes("\n");
            out.writeBytes("exit\n\n");
            out.writeBytes("exit\n\n");
            out.flush();
            out.close();
            if (!root && p.waitFor() != 0) {
                p.destroy();
                Log.w("KernelProrp", propName + "read error！");
                return getProp(propName, true);
            } else  {
                String line;
                StringBuilder stringBuilder = new StringBuilder();
                InputStream inputstream = p.getInputStream();
                InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
                BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
                while ((line = bufferedreader.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append("\n");
                }
                out.close();
                bufferedreader.close();
                inputstream.close();
                inputstreamreader.close();
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                StringBuilder errors = new StringBuilder();
                while ((line = errorReader.readLine()) != null) {
                    errors.append(line);
                    errors.append("\n");
                }
                errorReader.close();

                p.destroy();
                if (errors.length() > 0) {
                    Log.e("KernelProps", "reader ["+propName+"] " + errors.toString());
                    return "";
                }
                return stringBuilder.toString().trim();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /**
     * 保存属性
     *
     * @param propName 属性名称（要永久保存，请以persist.开头）
     * @param value    属性值,值尽量是简单的数字或字母，避免出现错误
     */
    public static boolean setProp(String propName, String value) {
        try {
            Process p = Runtime.getRuntime().exec("sh");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.writeBytes("echo " + value + " > " + propName);
            out.writeBytes("\n");
            out.writeBytes("\n\n\n");
            out.writeBytes("exit\n");
            out.writeBytes("exit\n");
            out.writeBytes("\n");
            out.flush();
            p.waitFor();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
