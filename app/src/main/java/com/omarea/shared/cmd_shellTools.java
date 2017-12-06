package com.omarea.shared;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by helloklf on 2016/8/4.
 */
public class cmd_shellTools {
    public Context context;
    public AppCompatActivity activity;
    public ProgressBar progressBar;

    public cmd_shellTools(AppCompatActivity activity, ProgressBar progressBar) {
        this.activity = activity;
        if (activity != null)
            this.context = activity.getApplicationContext();
        this.progressBar = progressBar;
    }

    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    //显示进度条
    public void ShowProgressBar() {
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                if (progressBar != null)
                    progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    //隐藏进度条
    public void HideProgressBar() {
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                if (progressBar != null)
                    progressBar.setVisibility(View.GONE);
            }
        });
    }

    //显示弹窗提示
    public void ShowDialogMsg(final String title, final String msg) {
        if (activity != null)
            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle(title);
                    builder.setPositiveButton(android.R.string.yes, null);
                    builder.setMessage(msg + "\n");
                    builder.create().show();
                }
            });
    }

    void NoRoot() {
        ShowDialogMsg("请检查ROOT权限", "请检查是否已ROOT手机，并允许本应用访问ROOT权限！");
    }

    //获取SD卡可用空间
    public long GetSDFreeSizeMB() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        return stat.getAvailableBytes() / 1024 / 1024; //剩余空间
    }

    //打包当前系统为Zip
    public void Rom2Zip(boolean boot, boolean sys, boolean rec, boolean other, String romName) throws UnsupportedEncodingException {
        new Rom2ZipThread(boot, rec, sys, other, URLEncoder.encode(romName, "UTF-8")).start();
    }


    class Rom2ZipThread extends Thread {

        public Rom2ZipThread(boolean hasboot, boolean hasrec, boolean hassystem, boolean hasother, String name) {
            this.has_boot = hasboot;
            this.has_rec = hasrec;
            this.has_other = hasother;
            this.has_sys = hassystem;
            this.fileName = name.equals("") ? "newrom" : name;
        }

        String fileName = "newrom";
        boolean has_rec;
        boolean has_boot;
        boolean has_sys;
        boolean has_other;

        @Override
        public void run() {
            Process process = null;
            try {
                ShowProgressBar();
                ShowDialogMsg("这需要好些时间", "这个过程需要10-20分钟（根据您的设备性能），您可以后台本应用去做别的事情。\n给手机降降温，或许速度会更快！");
                process = Runtime.getRuntime().exec("su");
                DataOutputStream out = new DataOutputStream(process.getOutputStream());

                out.writeBytes("busybox --install /system/xbin\n");
                //out.writeBytes("busybox --install /system/bin\n");
                String cmd = "if [ ! -f \"/system/xbin/zip\" ]; then cp " + AppShared.INSTANCE.getBaseUrl() + "zip /system/xbin/zip; chmod 0755 /system/xbin/zip ;fi;";

                out.writeBytes(cmd);
                out.writeBytes("\n");
                out.writeBytes("rm -rf /sdcard/VBOOTROMCREATE\n");
                out.writeBytes("mkdir -p /sdcard/VBOOTROMCREATE\n");
                out.writeBytes("mkdir -p /sdcard/VBOOTROMCREATE/TMP\n");

                out.writeBytes("cp " + AppShared.INSTANCE.getBaseUrl() + "rom.zip /sdcard/VBOOTROMCREATE/newrom.zip\n");

                //boot
                if (has_boot)
                    out.writeBytes("dd if=/dev/block/bootdevice/by-name/boot of=/sdcard/VBOOTROMCREATE/TMP/boot.img\n");
                //rec
                if (has_rec)
                    out.writeBytes("dd if=/dev/block/bootdevice/by-name/recovery of=/sdcard/VBOOTROMCREATE/TMP/recovery.img\n");

                //system
                if (has_sys) {
                    out.writeBytes("dd if=/dev/block/bootdevice/by-name/system of=/sdcard/VBOOTROMCREATE/TMP/system.img\n");
                }

                //other
                if (has_other) {

                }

                //zip
                out.writeBytes("cd /sdcard/VBOOTROMCREATE/TMP\n");
                out.writeBytes("zip -1 -vD -r /sdcard/VBOOTROMCREATE/newrom.zip .\n");

                String outName = "/sdcard/" + fileName + ".zip";
                //clear
                out.writeBytes("mv /sdcard/VBOOTROMCREATE/newrom.zip " + outName + "\n");
                out.writeBytes("cd /system/bin\n");
                out.writeBytes("rm -rf /sdcard/VBOOTROMCREATE\n");

                out.writeBytes("\n");

                out.writeBytes("exit");
                out.writeBytes("\n");
                out.flush();

                if (process.waitFor() == 0) {
                    ShowDialogMsg("打包成功！", "rom已复制到SD卡根目录下！\n 路径：" + outName);
                } else {
                    ShowDialogMsg("打包失败！", "请检查空间是否足够。或向开发者反馈此问题！");
                }
            } catch (IOException e) {
                e.printStackTrace();
                NoRoot();
            } catch (InterruptedException e) {
                ShowDialogMsg("打包失败！", "请检查空间是否足够。或向开发者反馈此问题！");
                e.printStackTrace();
            } finally {
                HideProgressBar();
            }
        }
    }

    //pigz
    //tar
    //zip
    //gzip
    //bzip2
    //gunzip
    //gunzip -c /data/media/rom2/system.img.gz | dd of=/data/media/rom2/system.img
    //dd if=/data/media/rom2/system.img | gzip > /data/media/rom2/system.img.gz
    //dd if=/data/media/rom2/system.img | zip > /data/media/rom2/system.img.zip
    //在系统一下备份VBOOT系统二

    //执行命令
    public void DoCmd(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.writeBytes(cmd);
            out.writeBytes("\n");
            out.writeBytes("exit\n");
            out.writeBytes("exit\n");
            out.flush();
        } catch (IOException e) {
            NoRoot();
        }
    }


    //执行命令
    public void DoCmdSync(String cmd) {
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
            NoRoot();
        } catch (Exception e) {

        }
    }

    public String GetProp(String prop, String grep) {
        try {
            Process p = Runtime.getRuntime().exec("sh");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.writeBytes("if [ ! -f \"" + prop + "\" ]; then echo \"\"; exit 1; fi;\n");
            String cmd = ("cat " + prop) + ((grep != null && grep.length() > 0) ? (" | grep " + grep) : "");
            out.writeBytes(cmd);
            out.writeBytes("\n");
            out.writeBytes("exit\n");
            out.flush();
            out.close();

            InputStream inputstream = p.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

            String line;
            StringBuilder stringBuffer = new StringBuilder();
            while ((line = bufferedreader.readLine()) != null) {
                stringBuffer.append(line);
                stringBuffer.append("\n");
            }
            bufferedreader.close();
            inputstream.close();
            inputstreamreader.close();
            p.destroy();
            p.destroy();
            return stringBuffer.toString().trim();
        } catch (Exception e) {

        }
        return null;
    }
}
