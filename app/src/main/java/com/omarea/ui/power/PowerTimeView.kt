package com.omarea.ui.power

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.omarea.model.PowerHistory
import com.omarea.store.BatteryHistoryStore
import com.omarea.vtools.R

class PowerTimeView : View {
    private lateinit var storage: BatteryHistoryStore

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        invalidateTextPaintAndMeasurements()
        storage = BatteryHistoryStore(this.context)
    }

    private fun invalidateTextPaintAndMeasurements() {}


    fun getColorAccent(): Int {
        /*
        val typedValue = TypedValue()
        this.activity.theme.resolveAttribute(R.attr.colorAccent, typedValue, true)
        return typedValue.data
        */
        return resources.getColor(R.color.colorAccent)
    }

    /**
     * dp转换成px
     */
    private fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    private fun minutes2Str(minutes: Int): String {
        if (minutes >= 1140) {
            return "" + (minutes / 1140) + "d" + ((minutes % 1140) / 60) + "h"
        } else if (minutes > 60) {
            return "" + (minutes / 60) + "h" + (minutes % 60) + "m"
        } else if (minutes == 0) {
            return "0"
        }
        return "" + minutes + "m"
    }

    private var ladder = false // 阶梯模式
    private val paint = Paint()
    private val dashPathEffect = DashPathEffect(floatArrayOf(4f, 8f), 0f)
    private val perfectRange = intArrayOf(10, 25, 50, 75, 100, 150, 250, 300, 450, 600, 900, 1200, 1800, 2400)
    private fun getPerfectXMax(value: Double): Int {
        var lastPerfectValue = perfectRange.last()
        if (value > lastPerfectValue) {
            while (true) {
                lastPerfectValue += 600
                if (lastPerfectValue >= value) {
                    return lastPerfectValue
                }
            }
        } else {
            for (it in perfectRange) {
                if (value <= it) {
                    return it
                }
            }
        }
        return value.toInt()
    }

    public fun setLadder(ladder: Boolean) {
        if (this.ladder != ladder) {
            this.ladder = ladder
            invalidate()
        }
    }

    public fun getLadder(): Boolean {
        return this.ladder
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val samples = storage.curve

        paint.reset()
        val pointRadius = 4f
        paint.strokeWidth = 2f

        val dpSize = dp2px(this.context, 1f)
        val innerPadding = dpSize * 24f

        val startTime = samples.map { it.startTime }.min()
        val maxTime = samples.map { it.endTime }.max()
        val maxTimeMinutes = (if (startTime != null && maxTime != null) ((maxTime - startTime) / 60000.0).toDouble() else 30.0)
        val minutes = getPerfectXMax(maxTimeMinutes)

        val maxY = 101

        val ratioX = (this.width - innerPadding - innerPadding) * 1.0 / minutes // 横向比率
        val ratioY = ((this.height - innerPadding - innerPadding) * 1.0 / maxY).toFloat() // 纵向比率
        val startY = height - innerPadding

        val textSize = dpSize * 8.5f
        paint.textSize = textSize

        paint.textAlign = Paint.Align.CENTER

        val columns = 5
        val scaleX = (minutes / columns.toDouble())
        paint.strokeWidth = 1f
        paint.style = Paint.Style.FILL
        for (point in 0..columns) {
            val drawX = (point * scaleX * ratioX).toInt() + innerPadding
            paint.color = Color.parseColor("#888888")
            val text = minutes2Str((point * scaleX).toInt())
            canvas.drawText(
                    text,
                    drawX,
                    this.height - innerPadding + textSize + (dpSize * 2),
                    paint
            )
            paint.color = Color.parseColor("#40888888")
            canvas.drawLine(
                    drawX, innerPadding,
                    drawX, this.height - innerPadding, paint
            )
        }

        paint.strokeWidth = 2f
        paint.pathEffect = dashPathEffect
        paint.textAlign = Paint.Align.RIGHT
        for (point in 0..maxY) {
            paint.color = Color.parseColor("#888888")
            if (point % 25 == 0) {
                if (point > 0) {
                    canvas.drawText(
                        point.toString() + "%",
                        innerPadding - dpSize * 4,
                        innerPadding + ((maxY - point) * ratioY).toInt() + textSize / 2.2f,
                        paint
                    )
                }
                paint.strokeWidth = if (point == 0) pointRadius else 2f
                if (point == 0) {
                    paint.color = Color.parseColor("#888888")
                } else {
                    paint.color = Color.parseColor("#aa888888")
                }
                canvas.drawLine(
                        innerPadding,
                        innerPadding + ((maxY - point) * ratioY).toInt(),
                        (this.width - innerPadding),
                        innerPadding + ((maxY - point) * ratioY).toInt(),
                        paint
                )
            }
        }

        paint.reset()
        paint.color = getColorAccent()
        if (startTime != null) {
            val first = samples.first()
            val last = samples.last()
            val startX = ((first.startTime - startTime) / 60000f * ratioX).toFloat() + innerPadding
            val endX = ((last.endTime - startTime) / 60000f * ratioX).toFloat() + innerPadding
            var lastX = startX
            var lastY = startY - (first.capacity * ratioY)
            var lastSample = first

            paint.isAntiAlias = true
            paint.strokeWidth = 8f
            paint.style = Paint.Style.FILL
            /*
            val mShader: Shader = LinearGradient(
                0f, 0f, 40f, 60f,
                longArrayOf(Color.RED.toLong(), Color.GREEN.toLong(),
                Color.BLUE.toLong()), null, Shader.TileMode.REPEAT
            )
            */
            // paint.setShadowLayer(35f, 0f, 30f, Color.BLACK)
            if (ladder) {
                for (sample in samples) {
                    // 如果样本数据间隔过长（补假数据：虚线）
                    if ((sample.startTime - lastSample.endTime) > 10000) {
                        paint.pathEffect = dashPathEffect
                        val currentX = ((sample.startTime - startTime) / 60000f * ratioX).toFloat() + innerPadding
                        val currentY = startY - (sample.capacity * ratioY)
                        // 亮屏状态的样本显示高亮色，否则显示灰色
                        if (lastSample.screenOn && sample.screenOn) {
                            paint.color = Color.parseColor("#1474e4")
                        } else {
                            paint.color = Color.parseColor("#80808080")
                        }
                        canvas.drawLine(
                                lastX,
                                lastY,
                                currentX,
                                currentY,
                                paint
                        )
                        lastX = currentX
                        lastY = currentY
                    }
                    val currentX = ((sample.endTime - startTime) / 60000f * ratioX).toFloat() + innerPadding
                    val currentY = startY - (sample.capacity * ratioY)

                    paint.pathEffect = null
                    // 亮屏状态的样本显示高亮色，否则显示灰色
                    if (sample.screenOn) {
                        paint.color = Color.parseColor("#1474e4")
                    } else {
                        paint.color = Color.parseColor("#80808080")
                    }

                    canvas.drawLine(
                            lastX,
                            lastY,
                            currentX,
                            currentY,
                            paint
                    )
                    lastX = currentX
                    lastY = currentY
                    lastSample = sample
                }
            } else {
                for (sample in samples) {
                    val currentX = ((sample.startTime - startTime) / 60000f * ratioX).toFloat() + innerPadding
                    val currentY = startY - (sample.capacity * ratioY)
                    // 如果样本数据间隔过长
                    if ((sample.startTime - lastSample.endTime) > 10000) {

                        // 先绘线到上个sample的endTime位置
                        paint.pathEffect = null
                        if (lastSample.screenOn) {
                            paint.color = Color.parseColor("#1474e4")
                        } else {
                            paint.color = Color.parseColor("#80808080")
                        }
                        val lastSampleX = ((lastSample.endTime - startTime) / 60000f * ratioX).toFloat() + innerPadding
                        val lastSampleY = startY - (lastSample.capacity * ratioY)
                        canvas.drawLine(lastX, lastY, lastSampleX, lastSampleY, paint)
                        lastX = lastSampleX
                        lastY = lastSampleY

                        // 接下来是补假数据（显示虚线）
                        paint.pathEffect = dashPathEffect
                        // 亮屏状态的样本显示高亮色，否则显示灰色
                        if (lastSample.screenOn) {
                            paint.color = Color.parseColor("#1474e4")
                        } else {
                            paint.color = Color.parseColor("#80808080")
                        }
                    } else {
                        paint.pathEffect = null
                        // 亮屏状态的样本显示高亮色，否则显示灰色
                        if (lastSample.screenOn) {
                            paint.color = Color.parseColor("#1474e4")
                        } else {
                            paint.color = Color.parseColor("#80808080")
                        }
                    }

                    canvas.drawLine(lastX, lastY, currentX, currentY, paint)
                    lastX = currentX
                    lastY = currentY
                    lastSample = sample
                }
                paint.pathEffect = null
                // 亮屏状态的样本显示高亮色，否则显示灰色
                if (lastSample.screenOn) {
                    paint.color = Color.parseColor("#1474e4")
                } else {
                    paint.color = Color.parseColor("#80808080")
                }
                canvas.drawLine(
                    lastX,
                    lastY,
                    endX,
                    startY - (last.capacity * ratioY),
                    paint
                )
            }
        }
    }
}
