package com.omarea.shell;

import android.content.Context;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Hello on 2017/12/03.
 */

public class SuDo {
    Context context;

    public SuDo(Context context) {
        this.context = context;
    }

    private void noRoot() {
        if (context != null)
            Toast.makeText(context, "没有ROOT权限无法运行", Toast.LENGTH_SHORT).show();
    }

    //执行命令
    public void execCmdSync(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.writeBytes(cmd);
            out.writeBytes("\n");
            out.writeBytes("exit\n");
            out.writeBytes("exit\n");
            out.flush();
            p.waitFor();
        } catch (IOException e) {
            noRoot();
        } catch (Exception e) {

        }
    }
}
