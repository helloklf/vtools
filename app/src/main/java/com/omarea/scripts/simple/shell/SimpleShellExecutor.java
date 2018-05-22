package com.omarea.scripts.simple.shell;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.omarea.vboot.R;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Hello on 2018/04/01.
 */

public class SimpleShellExecutor {
    private Context context;
    private Window window;
    private boolean started = false;

    public SimpleShellExecutor(Context context, Window window) {
        this.context = context;
        this.window = window;
    }

    /**
     * 执行脚本
     *
     * @param root
     * @param cmds
     * @param startPath
     */
    public boolean execute(Boolean root, String title, StringBuilder cmds, String startPath, Runnable onExit, HashMap<String, String> params) {
        if (started) {
            return false;
        }
        Process process = null;
        final File dir = context.getFilesDir();
        final String dirUri = dir.getAbsolutePath();
        ArrayList<String> envp = new ArrayList<String>();
        if (params != null) {
            for (String item : params.keySet()) {
                String value = params.get(item);
                if (value == null) {
                    value = "";
                }
                envp.add(item + "=" + value);
            }
        }
        envp.add("TEMP_DIR=" + dirUri + "/temp");
        envp.add("ANDROID_UID=" + dir.getParentFile().getParentFile().getName());
        envp.add("ANDROID_SDK=" + Build.VERSION.SDK_INT);
        envp.add("SDCARD_PATH=" + Environment.getExternalStorageDirectory().getAbsolutePath());

        Display display = window.getWindowManager().getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        Point point = new Point();
        display.getRealSize(point);

        envp.add("DISPLAY_DPI=" + dm.densityDpi);
        envp.add("DISPLAY_H=" + point.y);
        envp.add("DISPLAY_W=" + point.x);
        try {
            //process = Runtime.getRuntime().exec(root ? "su" : "bash");
            if(root) {
                process = Runtime.getRuntime().exec("su");
            } else {
                process = Runtime.getRuntime().exec("sh", envp.toArray(new String[envp.size()]));
            }
        } catch (Exception ex) {
            Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
            if (onExit != null)
                onExit.run();
        }

        if (process != null) {
            TextView textView = setLogView(title);

            final ShellHandler shellHandler = new SimpleShellHandler(textView);
            setHandler(process, shellHandler, onExit);

            final OutputStream outputStream = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            try {
                String start = startPath;
                if (startPath != null) {
                    start = startPath;
                } else {
                    start = context.getFilesDir().getAbsolutePath();
                }

                if(root) {
                    StringBuilder envpCmds = new StringBuilder();
                    if(envp.size() > 0) {
                        for (String param: envp) {
                            envpCmds.append("export ").append(param).append("\n");
                        }
                    }
                    dataOutputStream.write(envpCmds.toString().getBytes("UTF-8"));
                }
                dataOutputStream.write(String.format("cd '%s'\n", start).getBytes("UTF-8"));

                shellHandler.sendMessage(shellHandler.obtainMessage(ShellHandler.EVENT_START, "shell@android:" + start + " $\n\n"));
                shellHandler.sendMessage(shellHandler.obtainMessage(ShellHandler.EVENT_WRITE, cmds.toString()));

                dataOutputStream.writeBytes("sleep 0.2;\n");
                dataOutputStream.write(cmds.toString().replaceAll("\r\n", "\n").replaceAll("\r\t", "\t").getBytes("UTF-8"));
                dataOutputStream.writeBytes("\n\n");
                dataOutputStream.writeBytes("sleep 0.2;\n");
                dataOutputStream.writeBytes("exit\n");
                dataOutputStream.writeBytes("exit\n");
                dataOutputStream.flush();
            } catch (Exception ex) {
                process.destroy();
            }
            started = true;
        }
        return started;
    }

    /**
     * 创建并获取日志输出界面
     *
     * @return
     */
    private TextView setLogView(String title) {
        if (title == null) {
            title = context.getString(R.string.shell_executor);
        }
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.dialog_shell_executor, null);
        TextView textView = (TextView) view.findViewById(R.id.shell_output);
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(view)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create()
                .show();
        return textView;
    }

    /**
     * 设置日志处理Handler
     *
     * @param process      Runtime进程
     * @param shellHandler ShellHandler
     */
    private void setHandler(Process process, final ShellHandler shellHandler, final Runnable onExit) {
        final InputStream inputStream = process.getInputStream();
        final InputStream errorStream = process.getErrorStream();
        final Thread reader = new Thread(new Runnable() {
            @Override
            public void run() {
                String line;
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    while ((line = bufferedReader.readLine()) != null) {
                        shellHandler.sendMessage(shellHandler.obtainMessage(ShellHandler.EVENT_REDE, line + "\n"));
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
                        shellHandler.sendMessage(shellHandler.obtainMessage(ShellHandler.EVENT_READ_ERROR, line + "\n"));
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
                    shellHandler.sendMessage(shellHandler.obtainMessage(ShellHandler.EVENT_EXIT, status));
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
