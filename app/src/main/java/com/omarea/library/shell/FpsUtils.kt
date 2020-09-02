package com.omarea.library.shell

import com.omarea.common.shell.KeepShell
import com.omarea.common.shell.KeepShellPublic.doCmdSync
import com.omarea.common.shell.RootFile.fileExists

/**
 * 帧率检测
 */
class FpsUtils {
    private var fpsFilePath: String? = null
    private var subStrCommand = "| awk '{print \$2}'"
    val currentFps: String?
        get() {
            if (fpsFilePath == null) {
                when {
                    fileExists("/sys/class/drm/sde-crtc-0/measured_fps") -> {
                        fpsFilePath = "/sys/class/drm/sde-crtc-0/measured_fps"
                    }
                    fileExists("/sys/class/graphics/fb0/measured_fps") -> {
                        fpsFilePath = "/sys/class/graphics/fb0/measured_fps"
                        subStrCommand = ""
                    }
                    else -> {
                        val keepShell = KeepShell()
                        try {
                            Thread(Runnable {
                                fpsFilePath = ""
                                keepShell.doCmdSync("find /sys -name measured_fps 2>/dev/null")
                                        .trim { it <= ' ' }.split("\n").filter { it.contains("crtc") }.min()?.run {
                                            fpsFilePath = this
                                        }

                                if (fpsFilePath == null || fpsFilePath == "") {
                                    keepShell.doCmdSync("find /sys -name fps 2>/dev/null")
                                            .trim { it <= ' ' }.split("\n").filter { it.contains("crtc") }.min()?.run {
                                                fpsFilePath = this
                                            }
                                }
                            }).start()
                        } catch (ex: Exception) {
                            fpsFilePath = ""
                        }
                    }
                }
            } else if (!fpsFilePath!!.isEmpty()) {
                return doCmdSync("cat $fpsFilePath $subStrCommand")
            }
            return null
        }
}