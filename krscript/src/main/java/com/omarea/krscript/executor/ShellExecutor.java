package com.omarea.krscript.executor;

import android.content.Context;
import android.widget.Toast;

import com.omarea.krscript.model.ShellHandlerBase;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * Created by Hello on 2018/04/01.
 */
public class ShellExecutor {
    private boolean started = false;

    /**
     * 执行脚本
     *
     * @return
     */
    public Process execute(Context context, Boolean interruptible, String cmds, Runnable onExit, HashMap<String, String> params, ShellHandlerBase shellHandlerBase) {
        if (started) {
            return null;
        }

        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
        } catch (Exception ex) {
            Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
            if (onExit != null)
                onExit.run();
        }

        if (process != null) {
            final Process finalProcess = process;
            final Runnable forceStopRunnable = interruptible ? (new Runnable() {
                @Override
                public void run() {
                    try {
                        finalProcess.destroy();
                    } catch (Exception ignored) {
                    }
                }
            }) : null;
            new SimpleShellWatcher().setHandler(process, shellHandlerBase, onExit);

            final OutputStream outputStream = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            try {
                shellHandlerBase.sendMessage(shellHandlerBase.obtainMessage(ShellHandlerBase.EVENT_START, "shell@android:\n"));
                shellHandlerBase.sendMessage(shellHandlerBase.obtainMessage(ShellHandlerBase.EVENT_START, cmds + "\n\n"));
                shellHandlerBase.onStart(forceStopRunnable);
                dataOutputStream.writeBytes("sleep 0.2;\n");

                ScriptEnvironmen.executeShell(context, dataOutputStream, cmds, params);
            } catch (Exception ex) {
                process.destroy();
            }
            started = true;
        }
        return process;
    }

}
