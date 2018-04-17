package com.omarea.vboot.addin

import android.app.AlertDialog
import android.content.Context

/**
 * Created by Hello on 2018/03/22.
 */

class GoogleFrameworkAddin(private var context: Context) : AddinBase(context) {
    fun showOption() {
        val arr = arrayOf("冻结谷歌基础4件套", "解冻")
        var index = 0
        AlertDialog.Builder(context)
                .setTitle("请选择操作")
                .setSingleChoiceItems(arr, index, { _, which ->
                    index = which
                })
                .setNegativeButton("确定", { _, _ ->
                    when (index) {
                        0 -> disableFramework()
                        1 -> enableFramework()
                    }
                })
                .create().show()
    }

    private fun disableFramework() {
        command = StringBuilder()
                .append("pm disable com.google.android.gsf;")
                .append("pm disable com.google.android.gsf.login;")
                .append("pm disable com.google.android.gms;")
                .append("pm disable com.android.vending;")
                .toString()

        super.run()
    }

    private fun enableFramework() {
        command = StringBuilder()
                .append("pm enable com.google.android.gsf;")
                .append("pm enable com.google.android.gsf.login;")
                .append("pm enable com.google.android.gms;")
                .append("pm enable com.android.vending;")
                .toString()

        super.run()
    }
}
