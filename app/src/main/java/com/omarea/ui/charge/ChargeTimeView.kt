package com.omarea.ui.charge

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.omarea.store.ChargeSpeedStore
import com.omarea.vtools.R

class ChargeTimeView : View {
    private lateinit var storage: ChargeSpeedStore
    private val dashPathEffect = DashPathEffect(floatArrayOf(4f, 8f), 0f)

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
        storage = ChargeSpeedStore(this.context)
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

    private val perfectRange = intArrayOf(45, 50, 75, 100, 150, 250, 300, 450, 600, 900, 1200, 1800, 2400)
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
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val samples = storage.chargeTime()
        samples.sortBy { it.capacity }

        val paint = Paint()
        paint.strokeWidth = 2f

        val dpSize = dp2px(this.context, 1f)
        val innerPadding = dpSize * 24f

        val startTime = samples.map { it.startTime }.min()
        val maxTime = samples.map { it.endTime }.max()

        val maxTimeMinutes:Double = (if (startTime != null && maxTime != null) {
            (maxTime - startTime) / 60000.0
        } else {
            30.0
        })
        var minutes: Int = getPerfectXMax(maxTimeMinutes)

        val maxY = 101

        val ratioX = (this.width - innerPadding - innerPadding) * 1.0 / minutes // 横向比率
        val ratioY = ((this.height - innerPadding - innerPadding) * 1.0 / maxY).toFloat() // 纵向比率
        val startY = height - innerPadding

        val pathFilterAlpha = Path()

        val textSize = dpSize * 8.5f
        paint.textSize = textSize

        paint.textAlign = Paint.Align.CENTER

        val columns = 10
        var scaleX = (minutes / columns.toDouble())

        for (point in 0..columns) {
            val drawX = (point * scaleX * ratioX).toInt() + innerPadding
            if (point % 2 == 0) {
                paint.color = Color.parseColor("#888888")
                if (point % 4 == 0) {
                    val text = minutes2Str((point * scaleX).toInt())
                    canvas.drawText(
                        text,
                        drawX,
                        this.height - innerPadding + textSize + (dpSize * 2),
                        paint
                    )
                }
                paint.strokeWidth = 2f
            } else {
                paint.strokeWidth = 1f
            }
            paint.color = Color.parseColor("#40888888")
            canvas.drawLine(
                drawX, innerPadding,
                drawX, this.height - innerPadding, paint
            )
        }

        paint.textAlign = Paint.Align.RIGHT
        paint.pathEffect = dashPathEffect
        for (point in 0..maxY) {
            paint.color = Color.parseColor("#888888")
            if (point % 20 == 0) {
                if (point > 0) {
                    canvas.drawText(
                            "$point%",
                            innerPadding - dpSize * 4,
                            innerPadding + ((maxY - point) * ratioY).toInt() + textSize / 2.2f,
                            paint
                    )
                }
                paint.strokeWidth = 2f
            } else {
                paint.strokeWidth = 1f
            }

            if (point % 10 == 0) {
                paint.color = Color.parseColor("#40888888")
                canvas.drawLine(
                        innerPadding,
                        innerPadding + ((maxY - point) * ratioY).toInt(),
                        (this.width - innerPadding),
                        innerPadding + ((maxY - point) * ratioY).toInt(),
                        paint
                )
            }
        }

        paint.pathEffect = null
        paint.color = Color.parseColor("#801474e4")
        if (startTime != null) {
            val first = samples.first()
            val last = samples.last()
            val startX = ((first.startTime - startTime) / 60000f * ratioX).toFloat() + innerPadding
            val endX = ((last.endTime - startTime) / 60000f * ratioX).toFloat() + innerPadding
            pathFilterAlpha.moveTo(startX, startY - (first.capacity * ratioY))
            for (sample in samples) {
                val currentX = ((sample.startTime - startTime) / 60000f * ratioX).toFloat() + innerPadding
                pathFilterAlpha.lineTo(currentX, startY - (sample.capacity * ratioY))
            }
            pathFilterAlpha.lineTo(endX, startY - (last.capacity * ratioY))
        }

        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f

        paint.color = Color.parseColor("#1474e4")
        canvas.drawPath(pathFilterAlpha, paint)

        // paint.textSize = dpSize * 12f
        // paint.textAlign = Paint.Align.RIGHT
        // paint.style = Paint.Style.FILL
        // canvas.drawText("电量/时间", width - innerPadding, innerPadding - (dpSize * 4f), paint)
    }
}
