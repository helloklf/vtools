package com.omarea.store

import android.content.Context
import com.omarea.common.shared.ObjectStorage
import com.omarea.common.shell.KeepShellPublic
import com.omarea.library.shell.CpuFrequencyUtils
import com.omarea.model.CpuStatus
import java.io.File

/**
 * 存储和读取CPU配置，在开机自启动时用于修改CPU频率和调度
 * Created by Hello on 2018/08/04.
 */
class CpuConfigStorage(private val context: Context) : ObjectStorage<CpuStatus>(context) {
    private val defaultFile = "cpuconfig.dat"
    fun default(): String {
        return defaultFile
    }

    fun load(configFile: String? = null): CpuStatus? {
        return super.load(if (configFile == null) defaultFile else configFile)
    }

    fun saveCpuConfig(status: CpuStatus?, configFile: String? = null): Boolean {
        val name = if (configFile == null) defaultFile else configFile
        removeCache(name)
        return super.save(status, name)
    }

    // 应用CPU配置参数
    fun applyCpuConfig(context: Context, configFile: String? = null) {
        val name = if (configFile == null) defaultFile else configFile

        val cacheName = getCacheName(name)
        if (File(cacheName).exists()) {
            KeepShellPublic.doCmdSync(cacheName)
        } else if (exists(name)) {
            load(name)?.run {
                val commands = CpuFrequencyUtils().buildShell(this).joinToString("\n")
                saveCache(commands, name)
                KeepShellPublic.doCmdSync(commands)
            }
        }
    }

    private fun removeCache(name: String) {
        remove("$name.sh")
    }

    private fun getCacheName(name: String): String {
        return getSaveDir("$name.sh")
    }

    private fun saveCache(shellContent: String, name: String) {
        val cacheConfig = getCacheName(name)
        val file = File(cacheConfig)
        file.writeText(shellContent, Charsets.UTF_8)
        file.setWritable(true)
        file.setExecutable(true, false)
        file.setReadable(true)
    }
}
