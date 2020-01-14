package com.omarea.gesture.shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ShellExecutor {
    private static String extraEnvPath = "";
    private static String defaultEnvPath = ""; // /sbin:/system/sbin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin

    public static void setExtraEnvPath(String extraEnvPath) {
        ShellExecutor.extraEnvPath = extraEnvPath;
    }

    private static String[] getEnv() {
        if (extraEnvPath != null && !extraEnvPath.isEmpty()) {
            if (defaultEnvPath.isEmpty()) {
                try {
                    Process process = Runtime.getRuntime().exec("sh");
                    OutputStream outputStream = process.getOutputStream();
                    outputStream.write("echo $PATH".getBytes());
                    outputStream.flush();
                    outputStream.close();

                    InputStream inputStream = process.getInputStream();
                    byte[] cache = new byte[2048];
                    int length = inputStream.read(cache);
                    inputStream.close();
                    process.destroy();

                    String path = new String(cache, 0, length).trim();
                    if (path.length() > 0) {
                        defaultEnvPath = path;
                    } else {
                        throw new RuntimeException("未能获取到$PATH参数");
                    }
                } catch (Exception ex) {
                    defaultEnvPath = "/sbin:/system/sbin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin";
                }
            }

            String path = defaultEnvPath;

            return new String[]{
                    "PATH=" + path + ":" + extraEnvPath
            };
        }

        return null;
    }

    private static Process getProcess(String run) throws IOException {
        String[] env = getEnv();
        Runtime runtime = Runtime.getRuntime();
        if (env != null) {
            return runtime.exec(run, getEnv());
        }
        return runtime.exec(run);
    }

    public static Process getSuperUserRuntime() throws IOException {
        return getProcess("su");
    }

    public static Process getRuntime() throws IOException {
        return getProcess("sh");
    }
}
