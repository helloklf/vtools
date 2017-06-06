package com.omarea.shell;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by helloklf on 2017/6/3.
 */

public class CheckRootStatus {
    //是否已经Root
    public boolean IsRoot() {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.writeBytes("\nsetenforce 0\nexit\nexit\n");
            out.flush();

            String msg = "";
            InputStream errorStream = p.getErrorStream();

            while ((msg = new BufferedReader(new InputStreamReader(errorStream)).readLine()) != null) {
                if (msg.toLowerCase().equals("permission denied")) {
                    p.destroy();
                    return false;
                }
                if(msg.equals("not found")){
                    p.destroy();
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            if (p != null)
                p.destroy();
            return false;
        }
    }
}
