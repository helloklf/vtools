package com.omarea.krscripts;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.omarea.shared.FileWrite;
import com.omarea.shared.MagiskExtend;
import com.omarea.shell.KeepShellPublic;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ScriptEnvironmen {
    private static final String envFile = "krshell_environment.sh";
    private static boolean inited = false;
    private static String environmentPath = "";

    public static boolean init(Context context) {
        if (inited) {
            return true;
        }
        try {
            InputStream inputStream = context.getAssets().open(envFile);
            byte[] bytes = new byte[inputStream.available()];
            long length = inputStream.read(bytes, 0, bytes.length);
            String envShell = new String(bytes, Charset.defaultCharset()).replaceAll("\r", "");


            final File dir = context.getFilesDir();
            final String dirUri = dir.getAbsolutePath();

            String busybox = FileWrite.INSTANCE.getPrivateFilePath(context, "busybox");

            envShell = envShell.replace("${TEMP_DIR}", "\"" + dirUri + "/temp\"")
                    .replace("${ANDROID_UID}", dir.getParentFile().getParentFile().getName())
                    .replace("${ANDROID_SDK}", "" + Build.VERSION.SDK_INT)
                    .replace("${SDCARD_PATH}", Environment.getExternalStorageDirectory().getAbsolutePath());
            if (new File(busybox).exists()) {
                envShell = envShell.replace("${ANDROID_SDK}", "" + busybox);
            } else {
                envShell = envShell.replace("${ANDROID_SDK}", "busybox");
            }

            if (MagiskExtend.moduleInstalled()) {
                envShell = envShell.replace("${MAGISK_PATH}", (MagiskExtend.MAGISK_PATH.endsWith("/") ? (MagiskExtend.MAGISK_PATH.substring(0, MagiskExtend.MAGISK_PATH.length() -1 )) : MagiskExtend.MAGISK_PATH));
            } else {
                envShell = envShell.replace("${MAGISK_PATH}", "");
            }

            inited = FileWrite.INSTANCE.writePrivateFile(envShell.getBytes(Charset.defaultCharset()), envFile, context);
            if (inited) {
                environmentPath = FileWrite.INSTANCE.getPrivateFilePath(context, envFile);
            }
            return inited;
        } catch (Exception ex) {
            return false;
        }
    }

    private static final String ASSETS_FILE = "file:///android_asset/";

    public static String getStartPath(Context context, String startPath) {
        String start = null;
        if (startPath != null && !startPath.isEmpty()) {
            start = startPath;
        } else {
            start = FileWrite.INSTANCE.getPrivateFileDir(context);
        }
        return start;
    }

    public static String md5(String string) {
        if (string.isEmpty()) {
            return "";
        }

        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return "";
    }

    private static String createShellCache(Context context, String script) {
        String md5 = md5(script);
        String outputPath = "/kr-script/cache/" + md5 + ".sh";
        if (new File(outputPath).exists()) {
            return outputPath;
        }

        byte[] bytes = ("#!/system/bin/sh\n\n" + script).getBytes();
        if (FileWrite.INSTANCE.writePrivateFile(bytes, outputPath, context)) {
            return FileWrite.INSTANCE.getPrivateFilePath(context, outputPath);
        }
        return "";
    }

    private static String extractScript(Context context, String fileName) {
        if (fileName.startsWith("file:///android_asset/")) {
            fileName = fileName.substring("file:///android_asset/".length());
        }
        return FileWrite.INSTANCE.writePrivateShellFile(fileName, fileName, context);
    }

    public static String getExecuteScript(Context context, String script) {
        if (!inited) {
            init(context);
        }

        if (script == null || script.isEmpty()) {
            return "";
        }

        String script2 = script.trim();
        String startPath = getStartPath(context, null);
        if (script2.startsWith(ASSETS_FILE)) {
            String path = extractScript(context, script2);
            return environmentPath + " \"" + path + "\"" + " \"" + startPath + "\"";
        } else {
            String path = createShellCache(context, script);
            return environmentPath + " \"" + path + "\"" + " \"" + startPath + "\"";
        }
    }

    public static String executeResultRoot(Context context, String script) {
        if (!inited) {
            init(context);
        }

        if (script == null || script.isEmpty()) {
            return "";
        }

        String script2 = script.trim();
        String startPath = getStartPath(context, null);
        if (script2.startsWith(ASSETS_FILE)) {
            String path = extractScript(context, script2);
            return executeShell(context, path, startPath);
        } else {
            String path = createShellCache(context, script);
            return executeShell(context, path, startPath);
        }
    }

    public static String executeShell(Context context, String scriptPath, String startPath) {
        if (!inited) {
            init(context);
        }

        return KeepShellPublic.INSTANCE.doCmdSync(environmentPath + " \"" + scriptPath + "\"" + " \"" + startPath + "\"");
    }
}

