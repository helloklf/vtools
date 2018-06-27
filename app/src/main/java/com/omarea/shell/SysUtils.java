package com.omarea.shell;

import android.util.Log;

import com.omarea.vboot.BuildConfig;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class SysUtils {
    private static String App_Tag = "Performance Tweaker";
    private static boolean debug = BuildConfig.DEBUG;

    public static String readOutputFromFile(String pathToFile) {
        StringBuilder buffer = new StringBuilder();
        String data = null;
        Process process;
        BufferedReader stdinput;
        if (debug) {
            Log.d(App_Tag, "Reading Output from " + pathToFile);
        }
        File file = new File(pathToFile);
        if (!(file.exists())) {
            return "";
        }
        if (file.canRead()) {
            try {
                process = Runtime.getRuntime().exec("cat " + pathToFile);
                stdinput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while ((data = stdinput.readLine()) != null) {
                    buffer.append(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            data = buffer.toString();
        } else {
            InputStream inputStream;
            DataOutputStream dos;

            try {
                process = Runtime.getRuntime().exec("su");
                dos = new DataOutputStream(process.getOutputStream());
                dos.writeBytes("cat " + pathToFile + " \n");
                dos.flush();
                dos.writeBytes("exit \n");
                dos.writeBytes("exit \n");
                dos.flush();
                dos.close();
                if (process.waitFor() == 0) {
                    inputStream = process.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        data = line;
                    }
                }
            } catch (IOException | InterruptedException ioe) {
                ioe.printStackTrace();
            }
        }
        return data;
    }

    public static boolean executeRootCommand(List<String> commands) {
        InputStream is;
        DataOutputStream dos;

        try {
            Process mProcess = Runtime.getRuntime().exec("su");
            if (mProcess == null) return false;
            dos = new DataOutputStream(mProcess.getOutputStream());
            for (String cmd : commands) {
                dos.writeBytes(cmd + "\n");
                dos.flush();
                if (debug) {
                    Log.d(App_Tag, cmd);
                }
            }
            dos.writeBytes("exit \n");
            dos.writeBytes("exit \n");
            dos.flush();
            if (mProcess.waitFor() == 0) {
                is = mProcess.getInputStream();
                printOutputOnStdout(is);
                return true;
            } else {
                is = mProcess.getErrorStream();
            }
            dos.close();
            if (is != null) {
                printOutputOnStdout(is);
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String executeCommandWithOutput(boolean root, String command) {
        DataOutputStream dos;
        InputStream is;
        try {
            Process process;
            process = root ? Runtime.getRuntime().exec("su") : Runtime.getRuntime().exec("sh");
            if (process == null) return "";
            dos = new DataOutputStream(process.getOutputStream());
            dos.write(command.getBytes("UTF-8"));
            dos.writeBytes("\nexit \n");
            dos.flush();
            dos.close();
            if (process.waitFor() == 0) {
                is = process.getInputStream();
                StringBuilder builder = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = br.readLine()) != null)
                    builder.append(line.trim()).append("\n");
                return builder.toString().trim();
            } else {
                is = process.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = br.readLine()) != null) Log.d("error", line);
            }

        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static void printOutputOnStdout(InputStream is) {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                Log.e(App_Tag, line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}