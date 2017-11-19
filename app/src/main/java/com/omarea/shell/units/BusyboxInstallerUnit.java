package com.omarea.shell.units;

import com.omarea.shared.cmd_shellTools;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Hello on 2017/11/01.
 */

public class BusyboxInstallerUnit {
    //安装Shell工具
    public void InstallShellTools() {
        new InstallShellToolsThread().start();
    }

    class InstallShellToolsThread extends Thread {
        @Override
        public void run() {
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream out = new DataOutputStream(process.getOutputStream());
                out.writeBytes("busybox --install /system/xbin\n");
                out.writeBytes("exit");
                out.writeBytes("\n");
                out.flush();
                if (process.waitFor() == 0) {
                    //ShowMsg("自动安装了Busybox工具箱等插件！",false);
                } else {
                    //ShowMsg("没有安装Busybox，部分功能可能无法正常使用！", false);
                }
                process.destroy();
            } catch (Exception e) {

            }
        }
    }
}
