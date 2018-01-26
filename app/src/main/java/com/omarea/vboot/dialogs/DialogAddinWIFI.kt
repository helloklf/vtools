package com.omarea.vboot.dialogs

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.omarea.shell.SysUtils

/**
 * Created by Hello on 2018/01/22.
 */

class DialogAddinWIFI(private var context: Context) {
    fun show() {
        var wifiInfo = SysUtils.executeCommandWithOutput(true, "cat /data/misc/wifi/wpa_supplicant.conf")
        if (wifiInfo != null && wifiInfo.length > 0) {
            val infos = wifiInfo.split("\n\n")
            val sb = StringBuilder()

            for (row in infos) {
                if (row.trim().startsWith("network=")) {
                    sb.append(row)
                }
            }

            wifiInfo = sb.toString()
                    .replace(Regex("[\\s\\t]{0,}network=\\{"), "\n")
                    .replace(Regex("[\\s\\t]{0,}bssid=.*"), "") //bssid
                    .replace(Regex("[\\s\\t]{0,}ssid="), "\n网络：") //SSID
                    .replace(Regex("[\\s\\t]{0,}psk="), "\n密码：") //密码
                    .replace(Regex("[\\s\\t]{0,}priority=.*"), "") //优先级
                    .replace(Regex("[\\s\\t]{0,}key_mgmt=.*"), "") //加密方式
                    .replace(Regex("[\\s\\t]{0,}id_str=.*"), "") //idstr
                    .replace(Regex("[\\s\\t]{0,}disabled=.*"), "") //disabled
                    .replace(Regex("\\}"), "")
                    .replace("\"", "")
                    .trim()

            AlertDialog.Builder(context)
                    .setTitle("已保存的WIFI记录")
                    .setMessage(wifiInfo)
                    .setNeutralButton("确定", { _, _ -> }).create().show()
        } else {
            Toast.makeText(context, "没有读取到这个文件，也许不支持您的设备吧！", Toast.LENGTH_LONG).show()
        }
    }
}
