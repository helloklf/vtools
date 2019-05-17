package com.omarea.krscripts.simple.shell;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;
import com.omarea.krscripts.ExtractAssets;
import com.omarea.shared.FileWrite;
import com.omarea.shared.MagiskExtend;
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
    private boolean started = false;

    private static final String ASSETS_FILE = "file:///android_asset/";

    public SimpleShellExecutor(Context context) {
        this.context = context;
    }

    /**
     * 执行脚本
     */
    public boolean execute(String title, String cmds, String startPath, Runnable onExit, HashMap<String, String> params) {
        if (started) {
            return false;
        }

        Process process = null;
        final File dir = context.getFilesDir();
        final String dirUri = dir.getAbsolutePath();
        ArrayList<String> envp = new ArrayList<>();
        if (params != null) {
            for (String item : params.keySet()) {
                String value = params.get(item);
                if (value == null) {
                    value = "";
                }
                envp.add(item + "=" + value);
            }
        }
        if (MagiskExtend.moduleInstalled()) {
            envp.add("MAGISK_PATH=" + (MagiskExtend.MAGISK_PATH.endsWith("/") ? (MagiskExtend.MAGISK_PATH.substring(0, MagiskExtend.MAGISK_PATH.length() -1 )) : MagiskExtend.MAGISK_PATH));
        }
        envp.add("TEMP_DIR=" + dirUri + "/temp");
        envp.add("ANDROID_UID=" + dir.getParentFile().getParentFile().getName());
        envp.add("ANDROID_SDK=" + Build.VERSION.SDK_INT);
        envp.add("SDCARD_PATH=" + Environment.getExternalStorageDirectory().getAbsolutePath());
        String busyboxPath = FileWrite.INSTANCE.getPrivateFilePath(context, "busybox");
        if (new File(FileWrite.INSTANCE.getPrivateFilePath(context, "busybox")).exists()) {
            envp.add("BUSYBOX=" + busyboxPath);
        } else {
            envp.add("BUSYBOX=busybox");
        }

        try {
            process = Runtime.getRuntime().exec("su");
        } catch (Exception ex) {
            Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
            if (onExit != null)
                onExit.run();
        }

        if (process != null) {
            final Process finalProcess = process;
            final ShellHandler shellHandler = new SimpleShellHandler(context, title, new Runnable() {
                @Override
                public void run() {
                    try {
                        finalProcess.destroy();
                    } catch (Exception ex) {
                    }
                }
            });
            setHandler(process, shellHandler, onExit);

            final OutputStream outputStream = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            try {
                String start = null;
                if (startPath != null && !startPath.isEmpty()) {
                    start = startPath;
                } else {
                    start = FileWrite.INSTANCE.getPrivateFileDir(context);
                }

                StringBuilder envpCmds = new StringBuilder();
                if (envp.size() > 0) {
                    for (String param : envp) {
                        envpCmds.append("export ").append(param).append("\n");
                    }
                }
                dataOutputStream.write(envpCmds.toString().getBytes("UTF-8"));
                dataOutputStream.write(String.format("cd '%s'\n", start).getBytes("UTF-8"));

                shellHandler.sendMessage(shellHandler.obtainMessage(ShellHandler.EVENT_START, "shell@android:\n\n"));

                dataOutputStream.writeBytes("sleep 0.2;\n");
                if (cmds.startsWith(ASSETS_FILE)) {
                    String path = new ExtractAssets(context).extractScript(cmds);
                    String scripts = "chmod 755 \"" + path +
                            "\"\n" +
                            "sh \"" +
                            path +
                            "\"\n";
                    dataOutputStream.write((scripts).getBytes("UTF-8"));
                } else {
                    dataOutputStream.write(cmds.replaceAll("\r\n", "\n").replaceAll("\r\t", "\t").getBytes("UTF-8"));
                }
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
