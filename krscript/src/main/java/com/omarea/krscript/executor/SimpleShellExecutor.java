package com.omarea.krscript.executor;

import android.content.Context;
import android.widget.Toast;
import com.omarea.krscript.model.ConfigItemBase;
import com.omarea.krscript.model.ShellHandlerBase;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * Created by Hello on 2018/04/01.
 */
public class SimpleShellExecutor {
    private Context context;
    private boolean started = false;
    private static final String ASSETS_FILE = "file:///android_asset/";

    public SimpleShellExecutor(Context context) {
        this.context = context;
    }

    /**
     * 执行脚本
     */
    public boolean execute(ConfigItemBase configItem, String cmds, Runnable onExit, HashMap<String, String> params, ShellHandlerBase customHandler) {
        if (started) {
            return false;
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
            final Runnable forceStopRunnable = configItem.getInterruptible() ? (new Runnable() {
                @Override
                public void run() {
                    try {
                        finalProcess.destroy();
                    } catch (Exception ex) {
                    }
                }
            }) : null;
            final ShellHandlerBase shellHandlerBase = ((customHandler == null) ? (new SimpleShellHandler(context, configItem.getTitle(), configItem.getAutoOff())) : customHandler);
            setHandler(process, shellHandlerBase, onExit);

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
        return started;
    }

    /**
     * 设置日志处理Handler
     *
     * @param process      Runtime进程
     * @param shellHandlerBase ShellHandlerBase
     */
    private void setHandler(Process process, final ShellHandlerBase shellHandlerBase, final Runnable onExit) {
        final InputStream inputStream = process.getInputStream();
        final InputStream errorStream = process.getErrorStream();
        final Thread reader = new Thread(new Runnable() {
            @Override
            public void run() {
                String line;
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    while ((line = bufferedReader.readLine()) != null) {
                        shellHandlerBase.sendMessage(shellHandlerBase.obtainMessage(ShellHandlerBase.EVENT_REDE, line + "\n"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        final Thread readerError = new Thread(new Runnable() {
            @Override
            public void run() {
                String line;
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(errorStream, "UTF-8"));
                    while ((line = bufferedReader.readLine()) != null) {
                        shellHandlerBase.sendMessage(shellHandlerBase.obtainMessage(ShellHandlerBase.EVENT_READ_ERROR, line + "\n"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        final Process processFinal = process;
        Thread waitExit = new Thread(new Runnable() {
            @Override
            public void run() {
                int status = -1;
                try {
                    status = processFinal.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    shellHandlerBase.sendMessage(shellHandlerBase.obtainMessage(ShellHandlerBase.EVENT_EXIT, status));
                    if (reader.isAlive()) {
                        reader.interrupt();
                    }
                    if (readerError.isAlive()) {
                        readerError.interrupt();
                    }
                    onExit.run();
                }
            }
        });

        reader.start();
        readerError.start();
        waitExit.start();
    }
}
