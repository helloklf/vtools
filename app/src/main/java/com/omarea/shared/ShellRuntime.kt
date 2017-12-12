package com.omarea.shared

import java.io.DataOutputStream
import java.io.IOException

/**
 * Created by helloklf on 2017/6/3.
 */

class ShellRuntime {
    fun execute(cmd: String): Boolean? {
        try {
            val p = Runtime.getRuntime().exec("su")
            val dataOutputStream = DataOutputStream(p.outputStream)
            dataOutputStream.writeBytes(cmd)
            dataOutputStream.writeBytes("\n\nexit\nexit\n")
            dataOutputStream.flush()

            return p.waitFor() == 0
        } catch (e: InterruptedException) {
            return false;
        } catch (e: IOException) {
            return null;
        } finally {
        }
    }
}
