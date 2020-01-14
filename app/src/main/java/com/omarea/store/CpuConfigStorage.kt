package com.omarea.store

import android.content.Context
import android.widget.Toast
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KernelProrp
import com.omarea.model.CpuStatus
import com.omarea.shell_utils.CpuFrequencyUtil
import com.omarea.shell_utils.ThermalControlUtils
import com.omarea.vtools.R
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
            if (cpuState.coreControl.isNotEmpty()) {
                ThermalControlUtils.setCoreControlState(cpuState.coreControl == "1", context)
            }
            if (cpuState.msmThermal.isNotEmpty()) {
                ThermalControlUtils.setTheramlState(cpuState.msmThermal == "Y", context)
            }
            if (cpuState.vdd.isNotEmpty()) {
                ThermalControlUtils.setVDDRestrictionState(cpuState.vdd == "1", context)
            }

            // core online
            if (cpuState.coreOnline != null && cpuState.coreOnline.size > 0) {
                for (i in 0 until cpuState.coreOnline.size) {
                    CpuFrequencyUtil.setCoreOnlineState(i, cpuState.coreOnline[i])
                }
            }

            // CPU
            for (cluster in 0 until cpuState.cpuClusterStatuses.size) {
                val config = cpuState.cpuClusterStatuses[cluster]
                if (config.governor.isNotEmpty()) {
                    CpuFrequencyUtil.setGovernor(config.governor, cluster, context)
                }
                if (config.min_freq.isNotEmpty()) {
                    CpuFrequencyUtil.setMinFrequency(config.min_freq, cluster, context)
                }
                if (config.max_freq.isNotEmpty()) {
                    CpuFrequencyUtil.setMaxFrequency(config.max_freq, cluster, context)
                }
            }

            // Boost
            if (cpuState.boost.isNotEmpty()) {
                CpuFrequencyUtil.setSechedBoostState(cpuState.boost == "1", context)
            }
            if (cpuState.boostFreq.isNotEmpty()) {
                CpuFrequencyUtil.setInputBoosterFreq(cpuState.boostFreq)
            }
            if (cpuState.boostTime.isNotEmpty()) {
                CpuFrequencyUtil.setInputBoosterTime(cpuState.boostTime)
            }

            // GPU
            if (cpuState.adrenoGovernor.isNotEmpty()) {
                CpuFrequencyUtil.setAdrenoGPUGovernor(cpuState.adrenoGovernor)
            }
            if (cpuState.adrenoMinFreq.isNotEmpty()) {
                CpuFrequencyUtil.setAdrenoGPUMinFreq(cpuState.adrenoMinFreq)
            }
            if (cpuState.adrenoMaxFreq.isNotEmpty()) {
                CpuFrequencyUtil.setAdrenoGPUMaxFreq(cpuState.adrenoMaxFreq)
            }
            if (cpuState.adrenoMinPL.isNotEmpty()) {
                CpuFrequencyUtil.setAdrenoGPUMinPowerLevel(cpuState.adrenoMinPL)
            }
            if (cpuState.adrenoMaxPL.isNotEmpty()) {
                CpuFrequencyUtil.setAdrenoGPUMaxPowerLevel(cpuState.adrenoMaxPL)
            }
            if (cpuState.adrenoDefaultPL.isNotEmpty()) {
                CpuFrequencyUtil.setAdrenoGPUDefaultPowerLevel(cpuState.adrenoDefaultPL)
            }

            // exynos
            if (CpuFrequencyUtil.exynosHMP()) {
                CpuFrequencyUtil.setExynosHotplug(cpuState.exynosHotplug)
                CpuFrequencyUtil.setExynosHmpDown(cpuState.exynosHmpDown)
                CpuFrequencyUtil.setExynosHmpUP(cpuState.exynosHmpUP)
                CpuFrequencyUtil.setExynosBooster(cpuState.exynosHmpBooster)
            }

            if (!cpuState.cpusetBackground.isNullOrEmpty()) {
                KernelProrp.setProp("/dev/cpuset/background/cpus", cpuState.cpusetBackground)
            }
            if (!cpuState.cpusetSysBackground.isNullOrEmpty()) {
                KernelProrp.setProp("/dev/cpuset/system-background/cpus", cpuState.cpusetSysBackground)
            }
            if (!cpuState.cpusetForeground.isNullOrEmpty()) {
                KernelProrp.setProp("/dev/cpuset/foreground/cpus", cpuState.cpusetForeground)
            }
            if (!cpuState.cpusetForegroundBoost.isNullOrEmpty()) {
                KernelProrp.setProp("/dev/cpuset/foreground/boost/cpus", cpuState.cpusetForegroundBoost)
            }
            if (!cpuState.cpusetTopApp.isNullOrEmpty()) {
                KernelProrp.setProp("/dev/cpuset/top-app/cpus", cpuState.cpusetTopApp)
            }
        }
    }
}
