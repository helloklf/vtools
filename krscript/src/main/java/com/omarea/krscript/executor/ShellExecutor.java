package com.omarea.krscript.executor;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.omarea.krscript.model.RunnableNode;
import com.omarea.krscript.model.ShellHandlerBase;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * Created by Hello on 2018/04/01.
 */
public class ShellExecutor {
    private boolean started = false;
    private String sessionTag = "pio_" + System.currentTimeMillis();
    private void killProcess(Context context) {
        ScriptEnvironmen.executeResultRoot(
                context,
                String.format("kill -s 1 `pgrep -f %s`", sessionTag),
                null);
        // KeepShellPublic.INSTANCE.doCmdSync(String.format("kill -s 1 `pgrep -f %s`", sessionTag));
    }

    /**
     * 执行脚本
     */
    public Process execute(final Context context, RunnableNode nodeInfo, String cmds, Runnable onExit, HashMap<String, String> params, ShellHandlerBase shellHandlerBase) {
        if (started) {
            return null;
        }

        final Process process = ScriptEnvironmen.getRuntime();
        if (process == null) {
            Toast.makeText(context, "未能启动命令行进程", Toast.LENGTH_SHORT).show();
            if (onExit != null) {
                onExit.run();
            }
        } else {
            final Runnable forceStopRunnable = (nodeInfo.getInterruptable() || nodeInfo.getShell().equals(RunnableNode.Companion.getShellModeBgTask()))? (new Runnable() {
                @Override
                public void run() {
                    /*
                    // 没啥用，这个pid和在shell创建的子进程不是父子关系，杀死此进程对shell里创建的进程毫无影响
                    int pid = -1;
                    if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
                        try {
                            Class cl = process.getClass();
                            Field field = cl.getDeclaredField("pid");
                            field.setAccessible(true);
                            Object pidObject = field.get(process);
                            pid = (Integer) pidObject;
                        } catch (Exception ignored) {}
                    }
                    */
                    killProcess(context);

                    try {
                        process.getInputStream().close();
                    } catch (Exception ignored) {}
                    try {
                        process.getOutputStream().close();
                    } catch (Exception ignored) {}
                    try {
                        process.getErrorStream().close();
                    } catch (Exception ignored) {}

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            process.destroyForcibly();
                        } catch (Exception ex) {
                            Log.e("KrScriptError", "" + ex.getMessage());
                        }
                    } else {
                        try {
                            process.destroy();
                        } catch (Exception ex) {
                            Log.e("KrScriptError", "" + ex.getMessage());
                        }
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

                ScriptEnvironmen.executeShell(context, dataOutputStream, cmds, params, nodeInfo, sessionTag);
            } catch (Exception ex) {
                process.destroy();
            }
            started = true;
        }
        return process;
    }
}
