package com.omarea.shell;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Hello on 2017/8/8.
 */

public class Props {
    /**
     * 获取属性
     *
     * @param propName 属性名称
     * @return 内容
     */
    public static String getProp(String propName) {
        try {
            Process p = Runtime.getRuntime().exec("sh");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.writeBytes("getprop " + propName);
            out.writeBytes("\n");
            out.flush();

            InputStream inputstream = p.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

            String line;
            while ((line = bufferedreader.readLine()) != null) {
                out.writeBytes("exit\n");
                out.flush();
                out.close();
                bufferedreader.close();
                inputstream.close();
                inputstreamreader.close();
                p.destroy();
                return line;
            }
            p.destroy();
        } catch (Exception e) {

        }
        return null;
    }

    public static boolean setPorp(String propName, String value) {
        try {
            Process p = Runtime.getRuntime().exec("sh");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.writeBytes("setprop " + propName + " \"" + value + "\"");
            out.writeBytes("\n");
            out.flush();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
