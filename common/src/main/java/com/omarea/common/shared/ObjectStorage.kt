package com.omarea.common.shared

import android.content.Context
import android.widget.Toast
import java.io.*

open class ObjectStorage<T : Serializable>(private val context: Context) {
    private val objectStorageDir = "objects/"
    protected fun getSaveDir(configFile: String): String {
        return FileWrite.getPrivateFilePath(context, objectStorageDir + configFile)
    }

    public open fun load(configFile: String): T? {
        val file = File(getSaveDir(configFile))
        if (file.exists()) {
            var fileInputStream: FileInputStream? = null;
            var objectInputStream: ObjectInputStream? = null;
            try {
                fileInputStream = FileInputStream(file)
                objectInputStream = ObjectInputStream(fileInputStream)
                return objectInputStream.readObject() as T?
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

    public open fun save(obj: T?, configFile: String): Boolean {
        val file = File(getSaveDir(configFile))
        val parentFile = file.parentFile
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
        if (obj != null) {
            var fileOutputStream: FileOutputStream? = null
            var objectOutputStream: ObjectOutputStream? = null
            try {
                fileOutputStream = FileOutputStream(file)
                objectOutputStream = ObjectOutputStream(fileOutputStream)
                objectOutputStream.writeObject(obj)
                return true
            } catch (ex: Exception) {
                Toast.makeText(context, "存储配置失败！", Toast.LENGTH_SHORT).show()
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

    public open fun remove(configFile: String) {
        val file = File(getSaveDir(configFile))
        if (file.exists()) {
            file.delete()
        }
    }

    public open fun exists(configFile: String): Boolean {
        return File(getSaveDir(configFile)).exists()
    }
}
