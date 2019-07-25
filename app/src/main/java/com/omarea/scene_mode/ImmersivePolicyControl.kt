package com.omarea.scene_mode

import android.content.ContentResolver
import android.provider.Settings

/**
 * 沉浸式策略设置
 */
class ImmersivePolicyControl(private var contentResolver: ContentResolver) {
    val NAV = "immersive.navigation"
    val STATUS = "immersive.status"
    val FULL = "immersive.full"

    var nav = ""
    var status = ""
    var full = ""

    init {
        val r = Settings.Global.getString(contentResolver, "policy_control")
        if (r != null) {
            val policyControl = r.split(":")
            for (item in policyControl) {
                if (item.startsWith("immersive.full")) {
                    full = item
                } else if (item.startsWith("immersive.navigation")) {
                    nav = item
                } else if (item.startsWith("immersive.status")) {
                    status = item
                }
            }
        }
    }

    private fun isHide(prop: String, packageName: String): Boolean {
        if (prop.contains("-$packageName")) {
            return false
        } else if (prop.contains(packageName)) {
            return true
        } else if (prop.contains("$STATUS=*")) {
            return true
        } else if (prop.contains("$STATUS=apps")) {
            return true
        } else {
            return false
        }
    }

    fun isHideStatusOnly(packageName: String): Boolean {
        return isHide(status, packageName)
    }

    fun isHideNavbarOnly(packageName: String): Boolean {
        return isHide(nav, packageName)
    }

    fun isFullScreen(packageName: String): Boolean {
        return isHide(full, packageName)
    }

    fun hideNavBar(packageName: String): Boolean {
        if (isHideNavbarOnly(packageName) || isFullScreen(packageName)) {
            return true
        } else {
            if (nav.isEmpty()) {
                nav = "$NAV=$packageName"
            } else {
                nav = "$nav,$packageName"
            }
            return saveChange()
        }
    }

    fun showNavBar(packageName: String): Boolean {
        if (!isHideNavbarOnly(packageName) && !isFullScreen(packageName)) {
            return true
        } else if (isFullScreen(packageName)) {
            showAll(packageName)
            hideStatusBar(packageName)
        }
        if (nav.contains("$NAV=*") || nav.contains("$NAV=apps")) {
            nav = "$nav,-$packageName"
        } else if (nav.contains(packageName)) {
            nav = nav.replace(",$packageName", "").replace(packageName, "")
        }
        return saveChange()
    }

    fun hideStatusBar(packageName: String): Boolean {
        if (isHideStatusOnly(packageName) || isFullScreen(packageName)) {
            return true
        } else {
            if (status.isEmpty()) {
                status = "$STATUS=$packageName"
            } else {
                status = "$status,$packageName"
            }
            return saveChange()
        }
    }

    fun showStatusBar(packageName: String): Boolean {
        if (!isHideStatusOnly(packageName) && !isFullScreen(packageName)) {
            return true
        } else if (isFullScreen(packageName)) {
            showAll(packageName)
            hideNavBar(packageName)
        }
        if (status.contains("$STATUS=*") || status.contains("$STATUS=apps")) {
            status = "$status,-$packageName"
        } else if (status.contains(packageName)) {
            status = status.replace(",$packageName", "").replace(packageName, "")
        }
        return saveChange()
    }

    fun hideAll(packageName: String): Boolean {
        if (isFullScreen(packageName)) {
            return true
        } else {
            showStatusBar(packageName)
            showNavBar(packageName)
        }
        if (full.isEmpty()) {
            full = "$FULL=$packageName"
        } else {
            full = "$full,$packageName"
        }
        return saveChange()
    }

    fun showAll(packageName: String): Boolean {
        if (!isFullScreen(packageName)) {
            return true
        }
        if (full.contains("$FULL=*") || status.contains("$FULL=apps")) {
            full = "$full,-$packageName"
        } else if (full.contains(packageName)) {
            full = full.replace(",$packageName", "").replace(packageName, "")
        }
        return saveChange()
    }

    fun saveChange(): Boolean {
        val stringBuilder = StringBuilder()
        if (!status.isEmpty()) {
            stringBuilder.append(status)
        }
        if (!nav.isEmpty()) {
            if (!stringBuilder.isEmpty()) {
                stringBuilder.append(":")
            }
            stringBuilder.append(nav)
        }
        if (!full.isEmpty()) {
            if (!stringBuilder.isEmpty()) {
                stringBuilder.append(":")
            }
            stringBuilder.append(full)
        }
        return Settings.Global.putString(contentResolver, "policy_control", stringBuilder.toString())
    }
}
