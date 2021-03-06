package com.omarea.vtools.dialogs

import android.content.Context
import android.os.Build
import android.widget.Toast
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Created by Hello on 2018/01/22.
 */

class DialogAddinWIFI(private var context: Context) {
    fun showOld() {
        var wifiInfo = KeepShellPublic.doCmdSync("cat /data/misc/wifi/wpa_supplicant.conf")
        if (wifiInfo.isNotEmpty()) {
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
                    .replace(Regex("[\\s\\t]{0,}priority=.*"), "") //优先级
                    .replace(Regex("[\\s\\t]{0,}key_mgmt=.*"), "") //加密方式
                    .replace(Regex("[\\s\\t]{0,}id_str=.*"), "") //idstr
                    .replace(Regex("[\\s\\t]{0,}disabled=.*"), "") //disabled
                    .replace("}", "")
                    .replace("\"", "")
                    .trim()

            DialogHelper.alert(context, "已保存的WIFI记录", wifiInfo)
        } else {
            Toast.makeText(context, "没有读取到这个文件，也许不支持您的设备吧！", Toast.LENGTH_LONG).show()
        }
    }

    fun getInputStreamFromString(str: String): InputStream {
        return ByteArrayInputStream(str.toByteArray())
    }

    fun show() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                "/data/misc/apexdata/com.android.wifi/WifiConfigStore.xml"
            } else {
                "/data/misc/wifi/WifiConfigStore.xml"
            }
            val wifiInfo =  KeepShellPublic.doCmdSync("cat $path")
            if (wifiInfo.isNotEmpty()) {
                val factory = DocumentBuilderFactory.newInstance()
                val builder = factory.newDocumentBuilder()
                //获得Document对象
                val document = builder.parse(getInputStreamFromString(wifiInfo))
                val networkList = document.getElementsByTagName("WifiConfiguration")
                val stringBuild = StringBuilder()
                for (i in 0 until networkList.length) {
                    val wifi = networkList.item(i).childNodes
                    for (j in 0 until wifi.length) {
                        if (!wifi.item(j).hasChildNodes()) {
                            continue
                        }
                        val node = wifi.item(j).attributes.getNamedItem("name") ?: continue
                        if (node.nodeValue == "SSID") {
                            stringBuild.append("网络：")
                            stringBuild.append(wifi.item(j).textContent)
                            stringBuild.append("\n")
                        } else if (node.nodeValue == "PreSharedKey") {
                            stringBuild.append("密码：")
                            stringBuild.append(wifi.item(j).textContent)
                            stringBuild.append("\n")
                        }
                    }
                    stringBuild.append("\n\n")
                }

                DialogHelper.alert(context, "已保存的WIFI记录", stringBuild.toString().trim())
            } else {
                showOld()
            }
        } else {
            showOld()
        }
    }
}
