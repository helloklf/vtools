package com.omarea.ad

import android.content.Context
import android.os.Handler
import android.widget.Space
import android.widget.Toast
import com.omarea.shared.SpfConfig
import com.omarea.vtools.R
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

class AdHelper {
    fun checkUpdate(context: Context) {
        val handler = Handler();
        Thread(Runnable {
            //http://47.106.224.127/
            try {
                val url = URL("https://vtools.oss-cn-beijing.aliyuncs.com/vi/lastversion.json")
                val connection = url.openConnection()
                // 设置连接方式：get
                // connection.setRequestMethod("GET");
                // 设置连接主机服务器的超时时间：15000毫秒
                connection.connectTimeout = 15000
                // 设置读取远程返回的数据时间：60000毫秒
                connection.readTimeout = 60000
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
                val jsonObject = JSONObject(stringBuilder.toString().trim { it <= ' ' })
                val adConfig = context.getSharedPreferences(SpfConfig.AD_CONFIG, Context.MODE_PRIVATE)
                if (jsonObject.has("advertising")) {
                    adConfig.edit().putString(SpfConfig.AD_CONFIG_A_LINK, jsonObject.getString("advertising")).apply()
                } else {
                    adConfig.edit().putString(SpfConfig.AD_CONFIG_A_LINK, context.getString(R.string.promote_link)).apply()
                }
            } catch (ex: Exception) {

            }
        }).start()
    }
}
