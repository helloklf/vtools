package com.omarea.shell_utils;

import com.omarea.common.shell.KeepShell;
import com.omarea.common.shell.KeepShellPublic;
import com.omarea.common.shell.RootFile;

public class FpsUtils {
    private String fpsFilePath;
    public String getCurrentFps() {
        if (fpsFilePath == null) {
            if (RootFile.INSTANCE.fileExists("/sys/class/drm/sde-crtc-0/measured_fps")) {
                fpsFilePath = "/sys/class/drm/sde-crtc-0/measured_fps";
            } else {
                final KeepShell keepShell = new KeepShell();
                try {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String[] paths = keepShell.doCmdSync("find /sys -name measured_fps 2>/dev/null").trim().split("\n");
                            if (paths.length > 0) {
                                for (String path: paths) {
                                    if(path.contains("sde-crtc-0")) {
                                        fpsFilePath = path;
                                        break;
                                    }
                                }
                                if (fpsFilePath == null) {
                                    fpsFilePath = paths[0];
                                }
                            } else {
                                fpsFilePath = "";
                            }
                        }
                    });
                    thread.start();
                    thread.wait(5000);
                } catch (Exception ex) {
                    fpsFilePath = "";
                }
            }
        } else if (!fpsFilePath.isEmpty()) {
            return KeepShellPublic.INSTANCE.doCmdSync("cat " + fpsFilePath + " | awk '{print $2}'");
        }
        return null;
    }
}
