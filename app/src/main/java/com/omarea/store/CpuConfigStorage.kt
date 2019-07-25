package com.omarea.store

import android.content.Context
import android.widget.Toast
import com.omarea.common.shared.FileWrite
import com.omarea.model.CpuStatus
import java.io.*

/**
 * 存储和读取CPU配置，在开机自启动时用于修改CPU频率和调度
 * Created by Hello on 2018/08/04.
 */
class CpuConfigStorage {
    fun loadBootConfig(context: Context): CpuStatus? {
        val bootConfig = com.omarea.common.shared.FileWrite.getPrivateFilePath(context, "cpuconfig.dat")
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

    fun saveBootConfig(context: Context, status: CpuStatus?): Boolean {
        val bootConfig = FileWrite.getPrivateFilePath(context, "cpuconfig.dat")
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
}
