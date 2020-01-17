package com.omarea.store

import android.content.Context
import com.omarea.model.CpuStatus
import com.omarea.shell_utils.CpuFrequencyUtil
import com.omarea.shell_utils.GpuUtils
import com.omarea.shell_utils.ThermalControlUtils

/**
 * 存储和读取CPU配置，在开机自启动时用于修改CPU频率和调度
 * Created by Hello on 2018/08/04.
 */
class CpuConfigStorage(private val context: Context) : ObjectStorage<CpuStatus>(context) {
    private val defaultFile = "cpuconfig.dat"

    fun load(configFile: String? = null): CpuStatus? {
        return super.load( if(configFile == null) defaultFile else configFile)
    }

    fun saveCpuConfig(status: CpuStatus?, configFile: String? = null): Boolean {
        return super.save(status, if(configFile == null) defaultFile else configFile)
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
