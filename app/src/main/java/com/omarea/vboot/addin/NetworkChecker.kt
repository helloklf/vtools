package com.omarea.vboot.addin

import android.content.Context

/**
 * Created by Hello on 2018/03/22.
 */

class NetworkChecker(private var context: Context) : AddinBase(context) {
    fun disableNetworkChecker() {
        command = StringBuilder()
                .append("settings put global airplane_mode_on 1;")
                .append("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true;")
                .append("settings put global captive_portal_mode 0;")
                .append("settings put global captive_portal_detection_enabled 0;")
                .append("settings put global captive_portal_server www.androidbak.net;")
                .append("settings put global airplane_mode_on 0;")
                .append("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false;")
                .toString()

        super.run()
    }
}