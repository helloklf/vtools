package com.omarea.library.shell

import android.content.Context
import android.os.Build
import com.omarea.common.shared.RawText
import com.omarea.common.shell.KeepShellPublic
import com.omarea.vtools.R

/**
 * 帧率检测 （这方案并不可靠）
 */
class SurfaceFlingerFpsUtils(private val context: Context) {
    // Android Oreo
    private val cmd = RawText.getRawText(context, R.raw.surface_flinger_latency)
    val filter = "^[1-9]{1,}[\\s][1-9]{1,}[\\s][1-9]{1,}".toRegex()
    val colSplit = "\\s".toRegex()

    fun getFps (): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keepShell = KeepShellPublic.getInstance("fps", true)
            val result = keepShell.doCmdSync(cmd)
            val rows = result.split("\n").filter {
                filter.matches(it)
            }
            // .split(" ".toRegex())

            if (rows.size > 2) {
                val firstFrame = rows.first().split(colSplit)
                val lastFrame = rows[rows.size - 2].split(colSplit)
                val startTime = firstFrame[0].toLong()
                val endTime = lastFrame[1].toLong()
                // 总耗时（纳秒）
                // val totalTimeNS = (endTime - startTime) / 1000000
                // 总耗时（毫秒）
                val totalTimeMS = (endTime - startTime) / 1000000
                // 总帧数
                val frames = rows.size - 1
                // 每帧耗时
                val frameTimeMS = totalTimeMS / frames
                return (1000 / frameTimeMS).toInt()
            }
        }

        return 0
    }
}