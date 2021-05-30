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

    private var fpsCommand2 = "service call SurfaceFlinger 1013"
    private var lastTime = -1L
    private var lastFrames = -1

    val currentFps: String?
        get() {
            // 优先使用系统帧率
            if (fpsCommand2.isNotEmpty()) {
                val result = doCmdSync(fpsCommand2).trim()
                if (result != "error" && !result.contains("Parcel")) {
                    fpsCommand2 = ""
                } else {
                    try {
                        val index = result.indexOf("(") + 1
                        val frames = Integer.parseInt(result.substring(index, index + 8), 16)
                        val time = System.currentTimeMillis()
                        var fps = 0F
                        if (lastTime > 0 && lastFrames > 0) {
                            fps = (frames - lastFrames) * 1000.0f / (time - lastTime)
                        }
                        lastFrames = frames
                        lastTime = time
                        return String.format("%.1f", fps)
                    } catch (ex: Exception) {
                        if (!(lastTime > 0 && lastFrames > 0)) {
                            fpsCommand2 = ""
                        }
                    }
                }
            }
            // 如果系统帧率不可用使用GPU的内核级帧数数据
            else if (fpsFilePath == null) {
                when {
                    fileExists("/sys/class/drm/sde-crtc-0/measured_fps") -> {
                        fpsFilePath = "/sys/class/drm/sde-crtc-0/measured_fps"
                    }
                    fileExists("/sys/class/graphics/fb0/measured_fps") -> {
                        fpsFilePath = "/sys/class/graphics/fb0/measured_fps"
                        subStrCommand = ""
                    }
                    else -> {
                        fpsFilePath = ""
                        val keepShell = KeepShell()
                        Thread(Runnable {
                            try {
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
                                if (fpsFilePath == null) {
                                    fpsFilePath = ""
                                }
                                keepShell.tryExit()
                            } catch (ex: Exception) {
                                fpsFilePath = ""
                            }
                        }).start()
                    }
                }
            }
            // 优先使用GPU的内核级帧数数据
            else if (!fpsFilePath.isNullOrEmpty()) {
                return doCmdSync("cat $fpsFilePath $subStrCommand")
            }
            return null
        }
}