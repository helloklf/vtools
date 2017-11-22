package com.omarea.shared

import android.content.res.AssetManager
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.nio.charset.Charset

/**
 * Created by helloklf on 2016/8/27.
 */
object AppShared {
    var baseUrl = "/sdcard/Android/data/com.omarea.vboot/"
    var datFile = baseUrl + "data.dat"


    val configData: JSONObject
        get() {
            val file = File(datFile)
            try {
                if (!file.exists()) {
                    File(baseUrl).mkdirs()
                    file.createNewFile()
                }

                val inputStream = FileInputStream(file)
                val dataInputStream = DataInputStream(inputStream)

                val datas = ByteArray(file.length().toInt())
                dataInputStream.read(datas)
                val json = String(datas, Charset.forName("UTF-8"))

                var jsonObject: JSONObject

                try {
                    jsonObject = JSONObject(json)
                } catch (e: JSONException) {
                    jsonObject = JSONObject()
                }

                return jsonObject
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return JSONObject()
        }

    fun setConfigData(jsonObject: JSONObject): Boolean {
        val file = File(datFile)
        try {
            val outputStream = FileOutputStream(file)
            val dataOutputStream = DataOutputStream(outputStream)

            val data = jsonObject.toString()
            dataOutputStream.writeBytes(data)

            dataOutputStream.flush()
            dataOutputStream.close()
            outputStream.close()

            return true
        } catch (e: IOException) {
        }

        return false
    }

    fun WriteFile(ass: AssetManager, file: String, hasExtName: Boolean) {
        try {
            val inputStream = ass.open(file)
            val datas = ByteArray(2 * 1024 * 1024)
            val len = inputStream.read(datas)

            /*
            //获取SD卡的目录
            File sdCardDir = Environment.getExternalStorageDirectory();
            File targetFile = new File(sdCardDir.getCanonicalPath() + "shelltoolsfile.zip");
            //以指定文件创建RandomAccessFile对象
            RandomAccessFile raf = new RandomAccessFile(targetFile, "rw");
            //将文件记录指针移动到最后
            raf.seek(targetFile.length());
            //输出文件内容
            raf.write(datas,0,len);
            raf.close();
            */
            val dir = File(AppShared.baseUrl)
            if (!dir.exists())
                dir.mkdirs()
            val filePath = AppShared.baseUrl + if (hasExtName)
                file
            else
                file.substring(0, if (file.lastIndexOf(".") > 0) file.lastIndexOf(".") else file.length)

            val fileOutputStream = FileOutputStream(filePath)
            fileOutputStream.write(datas, 0, len)
            fileOutputStream.close()
            inputStream.close()
            //getApplicationContext().getClassLoader().getResourceAsStream("");
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun WriteFile(assetManager: AssetManager, file: String, outName: String) {
        try {
            val inputStream = assetManager.open(file)
            val f = File(file)
            val datas = ByteArray(2 * 1024 * 1024)
            val a = inputStream.available()
            val len = inputStream.read(datas)
            val dir = File(AppShared.baseUrl)
            if (!dir.exists())
                dir.mkdirs()
            val filePath = AppShared.baseUrl + outName

            val fileOutputStream = FileOutputStream(filePath)
            fileOutputStream.write(datas, 0, len)
            fileOutputStream.close()
            inputStream.close()
            //getApplicationContext().getClassLoader().getResourceAsStream("");
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}
