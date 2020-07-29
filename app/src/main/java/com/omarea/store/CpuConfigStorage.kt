package com.omarea.store

import android.content.Context
import com.omarea.common.shared.FileWrite
import com.omarea.common.shared.ObjectStorage
import com.omarea.common.shell.KeepShellPublic
import com.omarea.model.CpuStatus
import com.omarea.shell_utils.CpuFrequencyUtil
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
        remove(name + ".sh")
        return super.save(status, name)
    }

    // 应用CPU配置参数
    fun applyCpuConfig(context: Context, configFile: String? = null) {
        val name = if (configFile == null) defaultFile else configFile

        if (exists(name + ".sh")) {
            KeepShellPublic.doCmdSync(FileWrite.getPrivateFilePath(context, configFile + ".sh"))
        } else if (exists(name)) {
            load(name)?.run {
                val commands = CpuFrequencyUtil.buikdShell(this).joinToString("\n")
                saveCache(commands, name)
                KeepShellPublic.doCmdSync(commands)
            }
        }
    }

    private fun saveCache(shellContent: String, configFile: String) {
        val bootConfig = FileWrite.getPrivateFilePath(context, configFile + ".sh")
        val file = File(bootConfig)
        file.writeText(shellContent, Charsets.UTF_8)
        file.setWritable(true)
        file.setExecutable(true, false)
        file.setReadable(true)
    }
}
