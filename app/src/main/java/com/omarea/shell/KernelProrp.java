package com.omarea.shell;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
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
        if (!new File(propName).exists()) {
            return "";
        }
        try {
            Process p = Runtime.getRuntime().exec("sh");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.writeBytes("cat " + propName);
            out.writeBytes("\n");
            out.writeBytes("exit\n");
            out.writeBytes("exit\n");
            out.flush();

            InputStream inputstream = p.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedreader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            out.flush();
            out.close();
            bufferedreader.close();
            inputstream.close();
            inputstreamreader.close();
            p.destroy();
            return stringBuilder.toString().trim();
        } catch (Exception e) {

        }
        return null;
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
            out.flush();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
