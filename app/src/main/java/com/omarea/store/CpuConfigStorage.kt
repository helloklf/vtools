package com.omarea.store

import android.content.Context
import android.widget.Toast
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KernelProrp
import com.omarea.model.CpuStatus
import com.omarea.shell_utils.CpuFrequencyUtil
import com.omarea.shell_utils.GpuUtils
import com.omarea.shell_utils.ThermalControlUtils
import java.io.*

/**
 * 存储和读取CPU配置，在开机自启动时用于修改CPU频率和调度
 * Created by Hello on 2018/08/04.
 */
class CpuConfigStorage {
    private val defaultFile = "cpuconfig.dat"

    // 加载配置
    fun loadCpuConfig(context: Context, configFile: String? = null): CpuStatus? {
        val bootConfig = FileWrite.getPrivateFilePath(context, if(configFile == null) defaultFile else configFile)
        val file = File(bootConfig)
        if (file.exists()) {
            var fileInputStream: FileInputStream? = null;
            var objectInputStream: ObjectInputStream? = null;
            try {
                fileInputStream = FileInputStream(file)
                objectInputStream = ObjectInputStream(fileInputStream)
                return objectInputStream.readObject() as CpuStatus?
            } catch (ex: Exception) {
            } finally {
                try {
                    if (objectInputStream != null) {
                        objectInputStream.close()
                    }
                    if (fileInputStream != null) {
                        fileInputStream.close()
                    }
                } catch (ex: Exception) {
                }
            }
        }
        return null
    }

    // 保存CPU配置参数
    fun saveCpuConfig(context: Context, status: CpuStatus?, configFile: String? = null): Boolean {
        val bootConfig = FileWrite.getPrivateFilePath(context, if(configFile == null) defaultFile else configFile)
        val file = File(bootConfig)
        if (status != null) {
            var fileOutputStream: FileOutputStream? = null
            var objectOutputStream: ObjectOutputStream? = null
            try {
                fileOutputStream = FileOutputStream(file)
                objectOutputStream = ObjectOutputStream(fileOutputStream)
                objectOutputStream.writeObject(status)
                return true
            } catch (ex: Exception) {
                Toast.makeText(context, "更新配置为启动设置失败！", Toast.LENGTH_SHORT).show()
                return false
            } finally {
                try {
                    if (objectOutputStream != null) {
                        objectOutputStream.close()
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close()
                    }
                } catch (ex: Exception) {
                }
            }
        } else {
            if (file.exists()) {
                file.delete()
            }
        }
        return true
    }

    // 应用CPU配置参数
    fun applyCpuConfig(context: Context, cpuState: CpuStatus? = null) {
        if (cpuState != null) {
            // thermal
            ThermalControlUtils.setThermalParams(cpuState)

            // core online
            if (cpuState.coreOnline != null && cpuState.coreOnline.size > 0) {
                CpuFrequencyUtil.setCoresOnlineState(cpuState.coreOnline.toBooleanArray())
            }

            // CPU
            if (cpuState.cpuClusterStatuses != null && cpuState.cpuClusterStatuses.size > 0) {
                CpuFrequencyUtil.setClusterParams(cpuState.cpuClusterStatuses.toTypedArray())
            }

            // Boost
            CpuFrequencyUtil.setBoostParams(cpuState)

            // GPU
            GpuUtils.setAdrenoGPUParams(cpuState)

            // exynos
            if (CpuFrequencyUtil.exynosHMP()) {
                CpuFrequencyUtil.setExynosHotplug(cpuState.exynosHotplug)
                CpuFrequencyUtil.setExynosHmpDown(cpuState.exynosHmpDown)
                CpuFrequencyUtil.setExynosHmpUP(cpuState.exynosHmpUP)
                CpuFrequencyUtil.setExynosBooster(cpuState.exynosHmpBooster)
            }

            // cpuset
            CpuFrequencyUtil.setCpuSet(cpuState)
        }
    }
}
