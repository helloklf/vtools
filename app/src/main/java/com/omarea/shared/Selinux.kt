package com.omarea.shared

import com.omarea.shell.SysUtils

/**
 * Created by Hello on 2018/06/30.
 */

object Selinux {
    val isSeLinuxEnforcing: Boolean
        get() {
            val r = SysUtils.executeCommandWithOutput(false, "getenforce")
            return r.isEmpty() || r == "Enforcing" || r == "1"
        }
}
