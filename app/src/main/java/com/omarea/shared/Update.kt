package com.omarea.shared


import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.widget.Toast

import com.omarea.vtools.R

import org.json.JSONObject

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection

class Update {
    private fun currentVersionCode(context: Context): Int {
        val manager = context.packageManager
        var code = 0
        try {
            val info = manager.getPackageInfo(context.packageName, 0)
            code = info.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return code
    }

    fun checkUpdate(context: Context) {
        val  handler = Handler();
        Thread(Runnable {
            //http://47.106.224.127/
            try {
                val url = URL("http://47.106.224.127/publish/lastversion.json")
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
                if (jsonObject.has("versionCode")) {
                    val currentVersion = currentVersionCode(context)
                    if (currentVersion < jsonObject.getInt("versionCode")) {
                        handler.post {
                            AlertDialog.Builder(context)
                                    .setTitle("下载新版本" + jsonObject.getString("versionName") + " ？")
                                    .setMessage("更新内容：" +  "\n\n" + jsonObject.getString("message") + "\n\n如果下载速度过慢，也可以前往“酷安”自行下载")
                                    .setPositiveButton(R.string.btn_confirm) { dialog, which ->
                                        try {
                                            val intent = Intent()
                                            intent.data = Uri.parse("http://47.106.224.127/publish/app-release.apk")
                                            context.startActivity(intent)
                                        } catch (ex: java.lang.Exception) {
                                            Toast.makeText(context, "启动下载失败！", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .setNegativeButton(R.string.btn_cancel) { dialog, which -> }
                                    .create()
                                    .show()
                        }
                    }
                }
            } catch (ex: Exception) {
                handler.post {
                    Toast.makeText(context, "检查更新失败！\n" + ex.message, Toast.LENGTH_SHORT).show()
                }
            }
        }).start()
    }
}
