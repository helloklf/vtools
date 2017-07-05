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
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
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

    //判断当前是否是系统一
    public boolean CurrentSystemOne() {
        File mainDataDic = new File("/MainData");
        return !mainDataDic.exists();
    }


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

    //压缩System
    public void ZipSystem() {
        if (CurrentSystemOne()) {
            new ZipSystemThread().start();
        } else {
            ShowMsg("请在系统一下进行此操作！", false);
        }
    }

    //压缩system任务
    class ZipSystemThread extends Thread {
        public ZipSystemThread() {

        }

        public ZipSystemThread(int size) {
            if (size > 0)
                this.size = size;
        }

        int size = 0;

        @Override
        public void run() {
            ShowMsg("请稍等片刻，请不要关闭程序！", true);
            ShowProgressBar();
            try {
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream dos = new DataOutputStream(p.getOutputStream());
                dos.writeBytes("mkdir -p /data/media/rom2\n");
                dos.writeBytes("e2fsck -p -f /data/media/rom2/system.img\n");
                dos.writeChars("resize2fs /data/media/rom2/system.img 4096M\n");
                dos.writeBytes("e2fsck -p -f /data/media/rom2/system.img\n");


                if (size > 0) {
                    dos.writeChars("resize2fs /data/media/rom2/system.img " + size + "M\n");
                    dos.writeBytes("mkdir -p /data/tmp\n");
                    dos.writeBytes("mkdir -p /data/tmp/vbootsystem\n");
                    dos.writeBytes("busybox mount /data/media/rom2/system.img /data/tmp/vbootsystem\n");
                    dos.writeBytes("dd if=/dev/zero of=/data/tmp/vbootsystem/tmp.img\n");
                    dos.writeBytes("rm -f /data/tmp/vbootsystem/tmp.img\n");
                    dos.writeBytes("fuser -km /soft\n");
                    dos.writeBytes("busybox umount /data/tmp/vbootsystem\n");
                } else
                    dos.writeChars("resize2fs -M /data/media/rom2/system.img\n");

                //dos.writeChars("resize2fs /data/media/rom2/system.img "+ 2000 +"M\n");
                dos.writeChars("exit\n");
                dos.flush();
                if (p.waitFor() != 0) {
                    ShowDialogMsg("系统二System压缩失败！", "");
                } else {
                    ShowDialogMsg("系统二System压缩成功！", "");
                }
            } catch (IOException e) {
                e.printStackTrace();
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                HideProgressBar();
            }
        }
    }


    //获取VBOOT系统System容量
    int GetVBOOTSystemUseMB() throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec("su");
        DataOutputStream dos = new DataOutputStream(p.getOutputStream());
        dos.writeBytes("mkdir -p /data/media/rom2\n");
        dos.writeBytes("if [ ! -f \"/data/media/rom2/system.img\" ]; then dd if=/dev/zero of=/data/media/rom2/data.img count=64 bs=1048576;mke2fs -F -t ext4 /data/media/rom2/data.img;fi;");

        //echo `tune2fs -l /data/media/rom2/data11.img |grep "Block size"`  | cut -d ':' -f 2
        dos.writeBytes("e2fsck -p -f /data/media/rom2/system.img\n");
        dos.writeBytes("tune2fs -l /data/media/rom2/system.img |grep \"Block size\"\n");
        dos.writeBytes("resize2fs -P /data/media/rom2/system.img\n");

        dos.flush();

        InputStream inputstream = p.getInputStream();
        InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
        BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

        int size = 0;
        int bsize = 4096;
        String line;
        while ((line = bufferedreader.readLine()) != null) {
            if (line.toLowerCase().contains("block size")) {
                String sizeVal = line.substring(line.lastIndexOf(":") + 1).trim();
                bsize = Integer.parseInt(sizeVal);
            } else if (line.toLowerCase().contains("filesystem:")) {
                String sizeVal = line.substring(line.lastIndexOf("filesystem:") + 11).trim();
                size = Integer.parseInt(sizeVal) / 1000 * (bsize / 1024);//每个block大小为4K
                break;
            }
        }
        dos.writeBytes("exit\n");
        dos.flush();

        return size;
    }

    //压缩System2（预留100M）
    public void ZipSystem2() throws IOException, InterruptedException {
        if (CurrentSystemOne()) {
            new ZipSystemThread(GetVBOOTSystemUseMB() + 105).start();
        } else {
            ShowMsg("请在系统一下进行此操作！", false);
        }
    }

    //执行切换命令
    public void ToggleSystem() {
        ToggleShellThread t = new ToggleShellThread();
        t.start();
    }

    //系统切换命令
    class ToggleShellThread extends Thread {
        boolean flashFirstBoot;

        @Override
        public void run() {
            flashFirstBoot = (!CurrentSystemOne());

            ShowProgressBar();
            ShowMsg("正在切换系统，不要终止程序运行以免系统损坏，稍后自动重启，请稍等！", true);
            try {
                //首先获取SU权限
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream dos = new DataOutputStream(p.getOutputStream());

                //最高性能
                dos.writeBytes("echo 0 > /sys/module/msm_thermal/core_control/enabled\n");
                dos.writeBytes("echo \"0:3072000 1:3072000 2:3072000 3:3072000 4:3072000 5:3072000\" > /sys/module/msm_performance/parameters/cpu_min_freq\n");
                dos.writeBytes("echo \"0:3072000 1:3072000 2:3072000 3:3072000 4:3072000 5:3072000\" > /sys/module/msm_performance/parameters/cpu_max_freq\n");
                dos.writeBytes("echo 3072000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq\n");
                dos.writeBytes("echo 3072000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq\n");
                dos.writeBytes("echo 3072000 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_min_freq\n");
                dos.writeBytes("echo 3072000 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_max_freq\n");
                dos.writeBytes("echo 3072000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq\n");
                dos.writeBytes("echo 3072000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq\n");
                dos.writeBytes("echo 0 /sys/module/msm_thermal/core_control/cpus_offlined\n");
                dos.writeBytes("echo 4 > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus\n");
                dos.writeBytes("echo 2 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus\n");
                dos.writeBytes("echo 4 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus\n");
                dos.writeBytes("echo 1 > /proc/sys/kernel/sched_boost\n");
                dos.writeBytes("echo 1 > /sys/devices/system/cpu/cpu0/online\n");
                dos.writeBytes("echo 1 > /sys/devices/system/cpu/cpu1/online\n");
                dos.writeBytes("echo 1 > /sys/devices/system/cpu/cpu2/online\n");
                dos.writeBytes("echo 1 > /sys/devices/system/cpu/cpu3/online\n");
                dos.writeBytes("echo 1 > /sys/devices/system/cpu/cpu4/online\n");
                dos.writeBytes("echo 1 > /sys/devices/system/cpu/cpu5/online\n");
                dos.writeBytes("echo 1 > /sys/devices/system/cpu/cpu6/online\n");
                dos.writeBytes("echo 1 > /sys/devices/system/cpu/cpu7/online\n");

                if (flashFirstBoot) {
                    //提取备份BOOT
                    dos.writeChars("dd if=/dev/block/bootdevice/by-name/boot of=/MainData/media/rom2/boot2.img" + "\n");
                    //写入BOOT
                    dos.writeChars("dd if=/MainData/media/rom2/boot1.img of=/dev/block/bootdevice/by-name/boot" + "\n");
                } else {
                    //提取备份BOOT
                    dos.writeChars("dd if=/dev/block/bootdevice/by-name/boot of=/data/media/rom2/boot1.img" + "\n");
                    //写入BOOT
                    dos.writeChars("dd if=/data/media/rom2/boot2.img of=/dev/block/bootdevice/by-name/boot" + "\n");
                }

                //为了防止打断，不使用检测重启，而使用一次性完成
                dos.writeBytes("reboot\n");
                dos.flush();
            } catch (IOException e) {
                ToggleError();
            }

        }
    }

    void NoRoot() {
        ShowDialogMsg("请检查ROOT权限", "请检查是否已ROOT手机，并允许本应用访问ROOT权限！");
    }

    //系统切换失败...
    void ToggleError() {
        progressBar.post(new Runnable() {
            @Override
            public void run() {
                HideProgressBar();
                ShowDialogMsg("系统切换失败", "请检查ROOT权限，或联系开发人员 ！");
            }
        });
    }

    //VBOOT分区大小调整
    public void VBOOTDataReSize(int size) {
        ReSizeVBOOTDataThread resize = new ReSizeVBOOTDataThread(size);
        resize.start();
    }

    //获取VBOOT系统Data容量
    public int GetVBOOTDataSize() throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec("su");
        DataOutputStream dos = new DataOutputStream(p.getOutputStream());
        dos.writeBytes("du -m /data/media/rom2/data.img\n");
        dos.flush();

        InputStream inputstream = p.getInputStream();
        InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
        BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
        String text = bufferedreader.readLine().trim().toLowerCase();

        System.out.print(text);
        dos.writeBytes("exit\n");
        dos.flush();
        if (p.waitFor() == 0) {
            return Integer.parseInt(text.substring(0, text.indexOf("\t")));
        }
        return 0;
    }

    //获取SD卡可用空间
    public long GetSDFreeSizeMB() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        return stat.getAvailableBytes() / 1024 / 1024; //剩余空间
    }

    public long GetDirFreeSizeMB(String dir) {
        StatFs stat = new StatFs(dir);
        long size = stat.getAvailableBytes();
        return size / 1024 / 1024; //剩余空间
    }

    //获取VBOOT镜像可用空间
    public int GetImgFreeSizeMB() throws IOException {
        Process p = Runtime.getRuntime().exec("su");
        DataOutputStream dos = new DataOutputStream(p.getOutputStream());
        dos.writeBytes("mkdir -p /data/media/rom2\n");
        dos.writeBytes("if [ ! -f \"/data/media/rom2/data.img\" ]; then dd if=/dev/zero of=/data/media/rom2/data.img count=64 bs=1048576;mke2fs -F -t ext4 /data/media/rom2/data.img;fi;");

        //echo `tune2fs -l /data/media/rom2/data11.img |grep "Block size"`  | cut -d ':' -f 2
        dos.writeBytes("tune2fs -l /data/media/rom2/data.img |grep \"Block size\"\n");
        dos.writeBytes("tune2fs -l /data/media/rom2/data.img |grep \"Free blocks\"\n");

        dos.flush();

        InputStream inputstream = p.getInputStream();
        InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
        BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

        int size = 0;
        int bsize = 4096;
        String line;
        while ((line = bufferedreader.readLine()) != null) {
            if (line.toLowerCase().contains("block size")) {
                String sizeVal = line.substring(line.lastIndexOf(":") + 1).trim();
                bsize = Integer.parseInt(sizeVal);
            } else if (line.toLowerCase().contains("free blocks")) {
                String sizeVal = line.substring(line.lastIndexOf(":") + 1).trim();
                size = Integer.parseInt(sizeVal) / 1000 * (bsize / 1024);//每个block大小为4K
                break;
            }
        }
        dos.writeBytes("exit\n");
        dos.flush();

        return size;
    }

    //获取VBOOT系统Data镜像已用空间
    public int GetImgUseDataSizeMB() throws IOException {
        Process p = Runtime.getRuntime().exec("su");
        DataOutputStream dos = new DataOutputStream(p.getOutputStream());
        dos.writeBytes("mkdir -p /data/media/rom2\n");
        dos.writeBytes("if [ ! -f \"/data/media/rom2/data.img\" ]; then dd if=/dev/zero of=/data/media/rom2/data.img count=64 bs=1048576;mke2fs -F -t ext4 /data/media/rom2/data.img;fi;");

        //echo `tune2fs -l /data/media/rom2/data11.img |grep "Block size"`  | cut -d ':' -f 2
        dos.writeBytes("e2fsck -p -f /data/media/rom2/data.img\n");
        dos.writeBytes("tune2fs -l /data/media/rom2/data.img |grep \"Block size\"\n");
        dos.writeBytes("resize2fs -P /data/media/rom2/data.img\n");

        dos.flush();

        InputStream inputstream = p.getInputStream();
        InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
        BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

        int size = 0;
        int bsize = 4096;
        String line;
        while ((line = bufferedreader.readLine()) != null) {
            if (line.toLowerCase().contains("block size")) {
                String sizeVal = line.substring(line.lastIndexOf(":") + 1).trim();
                bsize = Integer.parseInt(sizeVal);
            } else if (line.toLowerCase().contains("filesystem:")) {
                String sizeVal = line.substring(line.lastIndexOf("filesystem:") + 11).trim();
                size = Integer.parseInt(sizeVal) / 1000 * (bsize / 1024);//每个block大小为4K
                break;
            }
        }
        dos.writeBytes("exit\n");
        dos.flush();

        return size;
    }

    //重设VBOOT Data大小
    public class ReSizeVBOOTDataThread extends Thread {
        public ReSizeVBOOTDataThread(int size) {
            this.newDataSizeMB = size;
        }

        //e2fsck -p /data/media/rom2/data.img
        //resize2fs /data/media/rom2/data.img 8192M
        int newDataSizeMB = 2048;
        int oldDataSizeMB = 2048;
        int oldDataUseMB = 2048;
        long freeSizeMB = 2048;
        long needSizeMB = 2048;

        @Override
        public void run() {
            freeSizeMB = GetSDFreeSizeMB(); //剩余空间

            try {
                oldDataSizeMB = GetVBOOTDataSize();//获取旧镜像

                if (oldDataSizeMB == newDataSizeMB) {
                    ShowMsg("与当前镜像容量相等，不需要做调整！", true);
                    return;
                } else if (newDataSizeMB < oldDataSizeMB) {//如果要压缩镜像
                    try {
                        oldDataUseMB = GetImgUseDataSizeMB(); //获取旧镜像已用空间
                        if (newDataSizeMB < oldDataUseMB) {
                            ShowDialogMsg("无法压缩镜像", "镜像已使用空间：" + oldDataUseMB + "MB\n指定的镜像容量：" + newDataSizeMB + "MB\n");
                            return;
                        } else {
                            needSizeMB = (newDataSizeMB - oldDataSizeMB);//计算所需空间
                        }
                    } catch (Exception e) {
                        ShowMsg("非常抱歉，无法缩小镜像容量，因为获取已用空间失败！", true);
                        return;
                    }
                } else {
                    needSizeMB = (newDataSizeMB - oldDataSizeMB);//所需的空间减去旧的镜像大小
                }
            } catch (InterruptedException e) {
                needSizeMB = newDataSizeMB;//如果无法获取旧的镜像，则需要分配所有空间
            } catch (IOException e) {
                NoRoot();
                return;
            }


            if (freeSizeMB < needSizeMB) {
                ShowMsg("空间不足，当前可用空间：" + freeSizeMB + "MB。请确保调整后有足够的剩余空间！\n还需：" + (freeSizeMB - needSizeMB) + "MB空间", true);
                return;
            }


            ShowDialogMsg("重要提示", "正在执行调整，请不要进行读写操作，以免由于空间不足导致系统二数据损坏！");
            ShowProgressBar();
            try {
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream dos = new DataOutputStream(p.getOutputStream());
                dos.writeBytes("mkdir -p /data/media/rom2\n");
                dos.writeBytes("if [ ! -f \"/data/media/rom2/data.img\" ]; then dd if=/dev/zero of=/data/media/rom2/data.img count=64 bs=1048576;mke2fs -F -t ext4 /data/media/rom2/data.img;fi;");
                dos.writeBytes("e2fsck -p -f /data/media/rom2/data.img\n");
                dos.writeBytes("resize2fs /data/media/rom2/data.img " + newDataSizeMB + "M\n");
                if (needSizeMB > 0) {
                    dos.writeBytes("mkdir -p /data/tmp\n");
                    dos.writeBytes("mkdir -p /data/tmp/vbootdata\n");
                    dos.writeBytes("busybox mount /data/media/rom2/data.img /data/tmp/vbootdata\n");
                    dos.writeBytes("dd if=/dev/zero of=/data/tmp/vbootdata/tmp.img\n");
                    dos.writeBytes("rm -f /data/tmp/vbootdata/tmp.img\n");
                    dos.writeBytes("fuser -km /soft\n");
                    dos.writeBytes("busybox umount /data/tmp/vbootdata\n");
                }

                //size smaller
                dos.writeChars("exit\n");
                dos.flush();
                if (p.waitFor() != 0) {
                    ShowDialogMsg("系统二Data调整失败，请重启手机再试试！", "");
                } else {
                    ShowDialogMsg("系统二Data调整成功，请重启手机！", "");
                }
            } catch (IOException e) {
                e.printStackTrace();
                NoRoot();
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (InterruptedException e) {
                e.printStackTrace();
                ShowDialogMsg("系统二Data调整失败，请重启手机再试试！", "");
            } finally {
                HideProgressBar();
            }
        }
    }

    //创建VBOOT分区
    public void CreateVBOOTData(int size) {
        CreateVBOOTDataThread resize = new CreateVBOOTDataThread(size);
        resize.start();
    }

    //创建VBOOT分区任务
    public class CreateVBOOTDataThread extends Thread {
        public CreateVBOOTDataThread(int size) {
            this.DataSizeMB = size;
        }

        int DataSizeMB = 2048;
        long needSizeMB = 2048;
        long oldDataSizeMB = 2048;
        long freeSizeMB = 2048;

        @Override
        public void run() {

            freeSizeMB = GetSDFreeSizeMB(); //剩余空间

            try {
                oldDataSizeMB = GetVBOOTDataSize();
                needSizeMB = DataSizeMB - oldDataSizeMB;
            } catch (IOException e) {
                NoRoot();
                e.printStackTrace();
                return;
            } catch (InterruptedException e) {
                needSizeMB = DataSizeMB;
                e.printStackTrace();
            }

            if (freeSizeMB < needSizeMB) {
                ShowDialogMsg("无法分配空间", "可用空间不足，所需空间：" + needSizeMB + "MB\n剩余空间：" + freeSizeMB + "MB\n还需空间：" + (needSizeMB - freeSizeMB) + "MB");
                return;
            }

            ShowMsg("请稍等片刻，请不要关闭程序！", true);
            ShowProgressBar();
            try {
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream dos = new DataOutputStream(p.getOutputStream());
                dos.writeBytes("mkdir -p /data/media/rom2\n");
                dos.writeChars("dd if=/dev/zero of=/data/media/rom2/data.img bs=1048576 count=" + DataSizeMB + "\n");
                dos.writeBytes("mke2fs -F -t ext4 /data/media/rom2/data.img\n");
                dos.writeChars("exit\n");
                dos.flush();
                if (p.waitFor() != 0) {
                    ShowDialogMsg("系统二Data创建失败！", "");
                } else {
                    ShowDialogMsg("系统二Data创建成功！", "");
                }
            } catch (IOException e) {
                e.printStackTrace();
                NoRoot();
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (InterruptedException e) {
                ShowDialogMsg("系统二Data创建失败！", "");
                e.printStackTrace();
            } finally {
                HideProgressBar();
            }
        }
    }

    //清空VBOOT分区
    public void WipeVBOOTData() {
        WipeVBOOTDataThread resize = new WipeVBOOTDataThread();
        resize.start();
    }

    class WipeVBOOTDataThread extends Thread {
        long oldDataSizeMB = 2048;

        @Override
        public void run() {

            try {
                oldDataSizeMB = GetVBOOTDataSize();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (oldDataSizeMB < 100) {
                ShowDialogMsg("操作失败", "无法获取当前VBOOT系统Data分区大小！");
                return;
            }

            ShowMsg("请稍等片刻，请不要关闭程序！", true);
            ShowProgressBar();
            try {
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream dos = new DataOutputStream(p.getOutputStream());
                dos.writeBytes("mke2fs -F -t ext4 /data/media/rom2/data.img\n");
                dos.writeChars("exit\n");
                dos.flush();
                if (p.waitFor() != 0) {
                    ShowDialogMsg("系统二Data清理失败！", "");
                } else {
                    ShowDialogMsg("系统二Data清理成功！", "");
                }
            } catch (IOException e) {
                NoRoot();
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (InterruptedException e) {
                ShowDialogMsg("系统二Data清理失败！", "");
            } finally {
                HideProgressBar();
            }
        }
    }


    //刷入Boot
    public void FlashBoot(String path) {
        new FlashBootThread(path).start();
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

    //删除VBOOT系统
    public void UnInstallVBOOT() {
        new UnInstallVBOOTThread().start();
    }

    class UnInstallVBOOTThread extends Thread {
        @Override
        public void run() {
            ShowProgressBar();
            try {
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream dos = new DataOutputStream(p.getOutputStream());
                dos.writeChars("rm -rf /data/media/rom2\n");
                dos.writeChars("exit\n");
                dos.flush();
                if (p.waitFor() != 0) {
                    ShowMsg("操作失败！", true);
                } else {
                    ShowDialogMsg("操作成功！", "双系统已从您的设备卸载，欢迎再次使用。如果您遇到问题，请向我们反馈！");
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

                if (CurrentSystemOne())
                    out.writeBytes("cp " + AppShared.INSTANCE.getBaseUrl() + "rom.zip /sdcard/VBOOTROMCREATE/newrom.zip\n");
                else
                    out.writeBytes("cp " + AppShared.INSTANCE.getBaseUrl() + "romvboot.zip /sdcard/VBOOTROMCREATE/newrom.zip\n");

                //boot
                if (has_boot)
                    out.writeBytes("dd if=/dev/block/bootdevice/by-name/boot of=/sdcard/VBOOTROMCREATE/TMP/boot.img\n");
                //rec
                if (has_rec)
                    out.writeBytes("dd if=/dev/block/bootdevice/by-name/recovery of=/sdcard/VBOOTROMCREATE/TMP/recovery.img\n");

                //system
                if (has_sys) {
                    if (CurrentSystemOne()) {
                        out.writeBytes("dd if=/dev/block/bootdevice/by-name/system of=/sdcard/VBOOTROMCREATE/TMP/system.img\n");
                    } else {
                        out.writeBytes("dd if=/MainData/media/rom2/system.img of=/sdcard/VBOOTROMCREATE/TMP/system.img\n");
                    }
                }

                //other
                if (has_other) {

                }


                //zip
                out.writeBytes("cd /sdcard/VBOOTROMCREATE/TMP\n");
                out.writeBytes("zip -1 -vD -r /sdcard/VBOOTROMCREATE/newrom.zip .\n");

                String outName = "/sdcard/" + (CurrentSystemOne() ? "" : "VBOOT_") + fileName + ".zip";
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

    public void SaveBoot(String path) {
        new SaveBootThread().start();
    }

    class SaveBootThread extends Thread {
        @Override
        public void run() {
            try {
                ShowProgressBar();
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream dataOutputStream = new DataOutputStream(p.getOutputStream());
                dataOutputStream.writeBytes("dd if=/dev/block/bootdevice/by-name/boot of=/sdcard/boot.img\n");
                dataOutputStream.writeBytes("exit\n");
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


    public void SaveRecovery(String path) {
        new SaveRecoveryThread().start();
    }

    class SaveRecoveryThread extends Thread {
        @Override
        public void run() {
            if (new ShellRuntime().execute("dd if=/dev/block/bootdevice/by-name/recovery of=/sdcard/recovery.img\n")) {
                ShowMsg("Recovery导出成功，已保存为/sdcard/recovery.img ！", true);
            } else {
                ShowMsg("Recovery导出失败！", true);
            }
            HideProgressBar();
        }
    }

    //安装Shell工具
    public void InstallShellTools() {
        new InstallShellToolsThread().start();
    }

    class InstallShellToolsThread extends Thread {
        public InstallShellToolsThread() {

        }

        @Override
        public void run() {
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream out = new DataOutputStream(process.getOutputStream());
                out.writeBytes("busybox --install /system/xbin\n");
                out.writeBytes("exit");
                out.writeBytes("\n");
                if (process.waitFor() == 0) {
                    //ShowMsg("自动安装了Busybox工具箱等插件！",false);
                } else {
                    ShowMsg("没有安装Busybox，部分功能可能无法正常使用！", false);
                }
            } catch (IOException e) {
                NoRoot();
                e.printStackTrace();
            } catch (InterruptedException e) {
                ShowMsg("没有安装Busybox，部分功能可能无法正常使用！", false);
                e.printStackTrace();
            }
        }
    }

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

    public String GetProp(String prop) {
        try {
            Process p = Runtime.getRuntime().exec("sh");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.writeBytes("if [ ! -f \"" + prop + "\" ]; then echo \"\"; exit 1; fi;\n");
            out.writeBytes("cat " + prop);
            out.writeBytes("\n");
            out.flush();

            InputStream inputstream = p.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

            String line;
            while ((line = bufferedreader.readLine()) != null) {
                out.writeBytes("exit\n");
                out.flush();
                out.close();
                bufferedreader.close();
                inputstream.close();
                inputstreamreader.close();
                p.destroy();
                return line;
            }
            p.destroy();
        } catch (Exception e) {

        }
        return null;
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

    public String getBatteryMAH() {
        String txt = GetProp("/sys/class/power_supply/battery/charge_full");
        if (txt == null || txt.trim().length() == 0)
            txt = GetProp("/sys/class/power_supply/battery/charge_full_design");
        if (txt == null || txt.trim().length() == 0)
            txt = "? mAh";
        if (txt.length() > 4)
            return txt.substring(0, 4) + " mAh";
        return txt + " mAh";
    }

    public int getChangeMAH() {
        // /sys/class/power_supply/battery/current_now
        String txt = GetProp("/sys/class/power_supply/battery/current_now");
        if (txt == null || txt.trim().length() == 0)
            txt = GetProp("/sys/class/power_supply/battery/BatteryAverageCurrent");

        if (txt == null)
            return Integer.MAX_VALUE;

        try {
            int ma = Math.abs(Integer.parseInt(txt));
            if (ma < 1000)
                return ma;
            return ma / 1000;
        } catch (Exception ex) {
            return 0;
        }
    }

    //是否有多系统
    public boolean IsDualSystem() {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.writeBytes("if [ ! -f \"/data/media/rom2/boot2.img\" -a ! -d \"/MainData\" ]; then echo 0; else echo 1; fi;");
            out.writeBytes("\n");
            out.flush();

            InputStream inputstream = p.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

            String line;
            while ((line = bufferedreader.readLine()) != null) {
                out.writeBytes("exit\n");
                out.flush();
                out.close();
                bufferedreader.close();
                inputstream.close();
                inputstreamreader.close();
                p.destroy();
                return line.trim().equals("1") ? true : false;
            }
        } catch (IOException e) {
            //NoRoot();
        }
        return false;
    }
}
