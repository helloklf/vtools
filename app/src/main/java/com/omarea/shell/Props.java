package com.omarea.shell;

import com.omarea.shared.helper.KeepShell;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Hello on 2017/8/8.
 */

public class Props {
    private static boolean isSeLinuxEnforcing() {
        String r = SysUtils.executeCommandWithOutput(false, "getenforce");
        return r.isEmpty() || r.equals("Enforcing") || r.equals("1");
    }

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
            out.writeBytes("\n");

            out.writeBytes("exit\n\n");
            out.writeBytes("exit\n\n");
            out.flush();
            out.close();

            StringBuilder lines = new StringBuilder();
            if (p.waitFor() == 0) {
                String line;

                InputStream inputstream = p.getInputStream();
                InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
                BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
                while ((line = bufferedreader.readLine()) != null) {
                    lines.append(line);
                }
            }
            p.destroy();
            return lines.toString().trim();
        } catch (Exception ignored) {

        }
        return "";
    }

    public static boolean setPorp(String propName, String value) {
        try {
            if(isSeLinuxEnforcing()) {
                return false;
            } else {
                Process p = Runtime.getRuntime().exec("sh");
                DataOutputStream out = new DataOutputStream(p.getOutputStream());
                out.writeBytes("setprop " + propName + " \"" + value + "\"");
                out.writeBytes("\n");
                out.writeBytes("exit\n");
                out.writeBytes("exit\n");
                out.writeBytes("\n");
                out.flush();
                p.waitFor();
                return true;
            }
        } catch (Exception ex) {
            return false;
        }
    }
}
