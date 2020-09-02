package com.omarea.library.calculator

import java.util.*

// 计算还有多久起床
// getUp = hours * 60 + minutes， 例如 6:30 表示为 6 * 60 + 30 = 390
class GetUpTime(private val getUp: Int) {
    // 现在时间
    val currentTime: Int
        get() {
            val now = Calendar.getInstance()
            return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        }

    // 距离下次起床还有多少分钟
    val minutes: Int
        get() {
            val nowTimeValue = currentTime
            // 距离起床的剩余时间（分钟）
            val timeRemaining = (
                    // 和计算闹钟距离下一次还有多久响的逻辑有点像
                    // 如果已经过了今天的起床时间，计算到明天的起床时间还有多久
                    if (nowTimeValue > getUp) {
                        // (24 * 60) => 1440
                        // (今天剩余时间 + 明天的起床时间) / 60分钟 计算小时数
                        ((1440 - nowTimeValue) + getUp)
                    }
                    // 如果还没过今天的起床时间
                    else {
                        (getUp - nowTimeValue)
                    })
            return timeRemaining
        }

    val nextGetUpTime: Long
        get() {
            return System.currentTimeMillis() + (minutes * 60 * 1000)
        }
}