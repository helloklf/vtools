package com.omarea.shell.units;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.omarea.shared.ShellRuntime;
import com.omarea.shared.cmd_shellTools;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Hello on 2017/11/01.
 */

public class BackupRestoreUnit {
    public ProgressBar progressBar;
    public Context context;
    public Activity activity;
    public BackupRestoreUnit(Activity activity, ProgressBar progressBar) {
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
    //显示文本消息
    public void ShowMsg(final String msg, final boolean longMsg) {
        if (context != null)
            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, msg, longMsg ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
                }
            });
    }

    //刷入Boot
    public void FlashBoot(String path) {
        new FlashBootThread(path).start();
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
    class FlashBootThread extends Thread {
        public FlashBootThread(String path) {
            this.path = path;
        }

        String path;

        @Override
        public void run() {
            ShowMsg("即将刷入\n" + path + "\n请勿操作手机！", true);
            ShowProgressBar();
            try {
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream dos = new DataOutputStream(p.getOutputStream());
                dos.writeChars("dd if=" + path + " of=/dev/block/bootdevice/by-name/boot\n");
                dos.writeChars("exit\n");
                dos.flush();
                if (p.waitFor() != 0) {
                    ShowMsg("镜像刷入失败！", true);
                } else {
                    ShowMsg("操作成功！", true);
                }
            } catch (IOException e) {
                e.printStackTrace();
                NoRoot();
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                HideProgressBar();
            }
        }
    }

    //刷入Recovery
    public void FlashRecovery(String path) {
        new FlashRecoveryThread(path).start();
    }

    class FlashRecoveryThread extends Thread {
        public FlashRecoveryThread(String path) {
            this.path = path;
        }

        String path;

        @Override
        public void run() {
            ShowMsg("即将刷入\n" + path + "\n请勿操作手机！", true);
            ShowProgressBar();
            try {
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream dos = new DataOutputStream(p.getOutputStream());
                dos.writeChars("dd if=" + path + " of=/dev/block/bootdevice/by-name/recovery\n");
                dos.writeChars("exit\n");
                dos.flush();
                if (p.waitFor() != 0) {
                    ShowMsg("镜像刷入失败", true);
                } else {
                    ShowMsg("操作成功！", true);
                }
            } catch (IOException e) {
                e.printStackTrace();
                NoRoot();
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                HideProgressBar();
            }
        }
    }


    public void SaveBoot() {
        new SaveBootThread().start();
    }

    class SaveBootThread extends Thread {
        @Override
        public void run() {
            try {
                ShowProgressBar();
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream dataOutputStream = new DataOutputStream(p.getOutputStream());
                dataOutputStream.writeBytes("dd if=/dev/block/bootdevice/by-name/boot of=/sdcard/boot.img;\n");
                dataOutputStream.writeBytes("exit\n");
                dataOutputStream.flush();
                if (p.waitFor() == 0) {
                    ShowMsg("Boot导出成功，保存在/sdcard/boot.img ！", true);
                } else {
                    ShowMsg("Boot导出失败！", true);
                }
            } catch (IOException e) {
                e.printStackTrace();
                NoRoot();
            } catch (InterruptedException e) {
                ShowMsg("Boot导出失败！", true);
                e.printStackTrace();
            } finally {
                HideProgressBar();
            }
        }
    }


    public void SaveRecovery() {
        new SaveRecoveryThread().start();
    }

    class SaveRecoveryThread extends Thread {
        @Override
        public void run() {
            ShowProgressBar();
            if (new ShellRuntime().execute("dd if=/dev/block/bootdevice/by-name/recovery of=/sdcard/recovery.img\n")) {
                ShowMsg("Recovery导出成功，已保存为/sdcard/recovery.img ！", true);
            } else {
                ShowMsg("Recovery导出失败！", true);
            }
            HideProgressBar();
        }
    }

}
