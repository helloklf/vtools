package com.omarea.ui.fps

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.omarea.store.FpsWatchStore
import com.omarea.vtools.R

class FpsTemperatureView : View {
    private lateinit var storage: FpsWatchStore

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
        storage = FpsWatchStore(this.context)
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

    private var sessionId:Long = 0L

    public fun setSessionId(sessionId: Long) {
        if (this.sessionId != sessionId) {
            this.sessionId = sessionId
            invalidate()
        }
    }

    public fun geSessionId(): Long {
        return this.sessionId
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (this.sessionId < 1) {
            return
        }

        val samples = storage.sessionFpsData(this.sessionId)

        paint.reset()
        val pointRadius = 4f
        paint.strokeWidth = 2f

        val dpSize = dp2px(this.context, 1f)
        val innerPadding = dpSize * 24f

        val minutes = (samples.size / 60) + (if (samples.size % 60 == 0) 0 else 1)

        val maxY = (samples.maxOrNull()!! + 1).toInt() // 101

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
                        point.toString(),
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
        val last = samples.last()
        val startX = innerPadding
        val endX = (minutes * ratioX).toFloat() + innerPadding
        var lastX = startX
        var lastY = startY - (maxY * ratioY)

        paint.isAntiAlias = true
        paint.strokeWidth = 8f
        paint.style = Paint.Style.FILL
        var index = 0

        paint.pathEffect = null
        paint.color = Color.parseColor("#80808080")
        for (sample in samples) {
            val currentX = (index / 60f * ratioX).toFloat() + innerPadding
            val currentY = startY - (sample * ratioY)

            canvas.drawLine(
                    lastX,
                    lastY,
                    currentX,
                    currentY,
                    paint
            )
            lastX = currentX
            lastY = currentY
            index ++
        }

        canvas.drawLine(
                lastX,
                lastY,
                endX,
                startY - (last * ratioY),
                paint
        )
    }
}