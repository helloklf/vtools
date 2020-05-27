package com.omarea.shell_utils

import com.omarea.common.shell.KeepShell
import com.omarea.common.shell.KeepShellPublic.doCmdSync
import com.omarea.common.shell.RootFile.fileExists

class FpsUtils {
    private var fpsFilePath: String? = null
    private var subStrCommand = "| awk '{print \$2}'"
    val currentFps: String?
        get() {
            if (fpsFilePath == null) {
                if (fileExists("/sys/class/drm/sde-crtc-0/measured_fps")) {
                    fpsFilePath = "/sys/class/drm/sde-crtc-0/measured_fps"
                } else if (fileExists("/sys/class/graphics/fb0/measured_fps")) {
                    fpsFilePath = "/sys/class/graphics/fb0/measured_fps"
                    subStrCommand = ""
                } else {
                    val keepShell = KeepShell()
                    try {
                        Thread(Runnable {
                            fpsFilePath = ""
                            keepShell.doCmdSync("find /sys -name measured_fps 2>/dev/null")
                                    .trim { it <= ' ' }.split("\n").filter { it.contains("crtc") }.sorted()
                                    .firstOrNull()?.run {
                                        fpsFilePath = this
                                    }

                            if (fpsFilePath == null || fpsFilePath == "") {
                                keepShell.doCmdSync("find /sys -name fps 2>/dev/null")
                                        .trim { it <= ' ' }.split("\n").filter { it.contains("crtc") }.sorted()
                                        .firstOrNull()?.run {
                                            fpsFilePath = this
                                        }
                            }
                        }).start()
                    } catch (ex: Exception) {
                        fpsFilePath = ""
                    }
                }
            } else if (!fpsFilePath!!.isEmpty()) {
                return doCmdSync("cat $fpsFilePath $subStrCommand")
            }
            return null
        }
}