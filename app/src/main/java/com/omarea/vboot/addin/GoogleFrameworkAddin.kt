package com.omarea.vboot.addin

import android.content.Context

/**
 * Created by Hello on 2018/03/22.
 */

class GoogleFrameworkAddin(private var context: Context) : AddinBase(context) {
    fun disableFramework() {
        command = StringBuilder()
                .append("pm disable com.google.android.gsf;")
                .append("pm disable com.google.android.gsf.login;")
                .append("pm disable com.google.android.gms;")
                .append("pm disable com.android.vending;")
                .toString()

        super.run()
    }

    fun enableFramework() {
        command = StringBuilder()
                .append("pm enable com.google.android.gsf;")
                .append("pm enable com.google.android.gsf.login;")
                .append("pm enable com.google.android.gms;")
                .append("pm enable com.android.vending;")
                .toString()

        super.run()
    }
}
