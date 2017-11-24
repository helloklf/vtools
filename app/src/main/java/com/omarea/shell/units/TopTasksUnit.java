package com.omarea.shell.units;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by helloklf on 2017/11/22.
 */

public class TopTasksUnit {
    public static void executeCommandWithOutput(Handler handler) {
        DataOutputStream dos;
        InputStream is;
        try {
            Process process;
            process = Runtime.getRuntime().exec("su");
            if (process == null) return;
            dos = new DataOutputStream(process.getOutputStream());
            dos.writeBytes("top -s cpu -d 3" + "\n");
            dos.flush();
            dos.close();

            is = process.getInputStream();
            StringBuilder builder = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            handler.sendMessage(handler.obtainMessage(0, process));
            boolean lastEmpty = false;
            int rowCout = 0;
            while ((line = br.readLine()) != null) {
                rowCout ++;
                if (line.equals("") && rowCout > 3) {
                    handler.sendMessage(handler.obtainMessage(1, builder.toString().trim()));
                    if ( builder.length() > 1)
                        builder.delete(0, builder.length()-1);
                    rowCout = 0;
                } else if (line.equals("")) {
                    if ( builder.length() > 1)
                        builder.delete(0, builder.length()-1);
                    rowCout = 0;
                } else {
                    builder.append(line.trim()).append("\n");
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}
