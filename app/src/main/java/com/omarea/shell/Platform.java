package com.omarea.shell;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by helloklf on 2017/6/3.
 */

public class Platform {
    //获取CPU型号，如msm8996
    public String GetCPUName() {
        try {
            Process p = Runtime.getRuntime().exec("sh");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.writeBytes("getprop ro.board.platform");
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
                return line.trim().toLowerCase();
            }
        } catch (IOException e) {
            //NoRoot();
        }
        return null;
    }
}
