package com.omarea.utils

import android.content.Context
import com.omarea.Scene
import com.omarea.store.AutoSkipConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

class AutoSkipCloudData {
    fun updateConfig(context: Context, showMsg: Boolean) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://vtools.oss-cn-beijing.aliyuncs.com/addin/auto-skip-config-v1.json")
                val connection = url.openConnection()
                // 设置连接方式：get
                // connection.setRequestMethod("GET");
                // 设置连接主机服务器的超时时间：15000毫秒
                connection.connectTimeout = 15000
                // 设置读取远程返回的数据时间：60000毫秒
                connection.readTimeout = 20000
                val bufferedReader = BufferedReader(InputStreamReader(connection.getInputStream()))
                val stringBuilder = StringBuilder()
                while (true) {
                    val line = bufferedReader.readLine()
                    if (line != null) {
                        stringBuilder.append(line)
                        stringBuilder.append("\n")
                    } else {
                        break
                    }
                }
                val data = JSONArray(stringBuilder.toString().trim { it <= ' ' })
                if (showMsg) {
                    Scene.toast("从云端获得 " + data.length() + " 条(自动跳过)数据")
                }
                val db = AutoSkipConfigStore(context)
                db.clearAll()
                for (index in 0 until data.length()) {
                    val row = data.getJSONObject(index)
                    row?.run {
                        db.addConfig(getString("activity"), getString("viewId"))
                    }
                }
            } catch (ex: Exception) {
                if (showMsg) {
                    Scene.toast("获取云端配置数据失败~")
                }
            }
        }
    }
}
