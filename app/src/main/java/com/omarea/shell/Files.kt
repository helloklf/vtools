package com.omarea.shell

import android.os.StatFs

/**
 * Created by Hello on 2017/11/01.
 */

object Files {
    fun getDirFreeSizeMB(dir: String): Long {
        val stat = StatFs(dir)
        val size = stat.availableBytes
        return size / 1024 / 1024 //剩余空间
    }
}
