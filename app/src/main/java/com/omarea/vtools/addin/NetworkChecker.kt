package com.omarea.vtools.addin

import android.content.Context

/**
 * Created by Hello on 2018/03/22.
 */

class NetworkChecker(private var context: Context) : AddinBase(context) {
    fun disableNetworkChecker() {
        //参考大佬原文 https://www.evil42.com/index.php/archives/17/
        command = StringBuilder()
                .append("settings put global airplane_mode_on 1;")
                .append("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true;")

                .append("settings put global captive_portal_server https://connect.rom.miui.com/generate_204;")
                .append("settings put global captive_portal_http_url https://connect.rom.miui.com/generate_204;")
                .append("settings put global captive_portal_https_url https://connect.rom.miui.com/generate_204;")
                .append("settings put global captive_portal_use_https 1;")
                .append("settings put global captive_portal_mode 1;")
                .append("settings put global captive_portal_detection_enabled 1;")

                .append("settings put global airplane_mode_on 0;")
                .append("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false;")
                .toString()

        super.run()
    }
}