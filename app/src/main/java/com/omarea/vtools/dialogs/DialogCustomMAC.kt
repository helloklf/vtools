package com.omarea.vtools.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import com.omarea.common.shell.KeepShellPublic
import com.omarea.store.SpfConfig
import com.omarea.vtools.R

/**
 * Created by Hello on 2018/01/17.
 */

class DialogCustomMAC(private var context: Context) {
    private var spf: SharedPreferences? = null

    init {
        spf = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
    }

    @SuppressLint("ApplySharedPref")
    fun modifyMAC() {
        val layoutInflater = LayoutInflater.from(context)
        val dialog = layoutInflater.inflate(R.layout.dialog_addin_mac, null)
        val macInput = dialog.findViewById(R.id.dialog_addin_mac_input) as EditText
        val autoChange = dialog.findViewById(R.id.dialog_addin_mac_autochange) as CheckBox
        macInput.setText(spf!!.getString(SpfConfig.GLOBAL_SPF_MAC, "ec:d0:9f:af:95:01"))
        autoChange.isChecked = spf!!.getBoolean(SpfConfig.GLOBAL_SPF_MAC_AUTOCHANGE, false)
        autoChange.setOnCheckedChangeListener({ buttonView, isChecked ->
            spf!!.edit().putBoolean(SpfConfig.GLOBAL_SPF_MAC_AUTOCHANGE, isChecked).commit()
        })

        val instance = AlertDialog.Builder(context).setTitle("自定义WIFI MAC").setView(dialog).setNegativeButton("确定", { _, _ ->
            val mac = macInput.text.toString().toLowerCase()
            if (!Regex("[\\w\\d]{2}:[\\w\\d]{2}:[\\w\\d]{2}:[\\w\\d]{2}:[\\w\\d]{2}:[\\w\\d]{2}$", RegexOption.IGNORE_CASE).matches(mac)) {
                Toast.makeText(context, "输入的MAC地址无效，格式应如 ec:d0:9f:af:95:01", Toast.LENGTH_LONG).show()
                return@setNegativeButton
            }
            spf!!.edit().putString(SpfConfig.GLOBAL_SPF_MAC, mac).commit()
            val r = KeepShellPublic.doCmdSync("chmod 0755 /sys/class/net/wlan0/address\n" +
                    "svc wifi disable\n" +
                    "ifconfig wlan0 down\n" +
                    "echo '$mac' > /sys/class/net/wlan0/address\n" +
                    "ifconfig wlan0 hw ether '$mac'\n" +
                    "chmod 0755 /sys/devices/soc/a000000.qcom,wcnss-wlan/wcnss_mac_addr\n" +
                    "echo '$mac' > /sys/devices/soc/a000000.qcom,wcnss-wlan/wcnss_mac_addr\n" +
                    "ifconfig wlan0 up\n" +
                    "svc wifi enable\n");
            if (r == "error") {
                Toast.makeText(context, "修改失败！", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "MAC已修改", Toast.LENGTH_SHORT).show()
            }
        }).create()
        instance.window!!.setWindowAnimations(R.style.windowAnim)
        instance.show()
    }
}
