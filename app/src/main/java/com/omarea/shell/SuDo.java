package com.omarea.shell;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 使用ROOT权限执行命令
 * Created by Hello on 2017/12/03.
 */

public class SuDo {
    private Context context;
    private Handler handler;

    public SuDo(Context context) {
        this.context = context;
    }

    private void noRoot() {
        if (context != null)
        {
            if (this.handler == null) {
                this.handler = new Handler(Looper.getMainLooper());
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "没有ROOT权限无法运行", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    //执行命令
    public boolean execCmdSync(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.write(cmd.getBytes("UTF-8"));
            out.writeBytes("\n");
            out.writeBytes("exit\n");
            out.writeBytes("exit\n");
            out.flush();
            p.waitFor();
            Log.d("r", "" + p.exitValue());
            return p.exitValue() == 0;
        } catch (IOException e) {
            noRoot();
        } catch (Exception ignored) {

        }
        return false;
    }

    public void execCmd(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.write(cmd.getBytes("UTF-8"));
            out.writeBytes("\n");
            out.writeBytes("exit\n");
            out.writeBytes("exit\n");
            out.flush();
        } catch (IOException e) {
            noRoot();
        } catch (Exception ignored) {

        }
    }
}
