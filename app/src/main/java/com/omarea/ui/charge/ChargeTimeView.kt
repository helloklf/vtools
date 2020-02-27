package com.omarea.ui.charge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.omarea.store.ChargeSpeedStore
import com.omarea.vtools.R

class ChargeTimeView : View {
    private lateinit var storage: ChargeSpeedStore

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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val samples = storage.chargeTime()
        samples.sortBy { it.capacity }

        val potintRadius = 4f
        val paint = Paint()
        paint.strokeWidth = 2f

        val dpSize = dp2px(this.context, 1f)
        val innerPadding = dpSize * 24f

        val startTime = samples.map { it.startTime }.min()
        val maxTime = samples.map { it.endTime }.max()
        var minutes: Int = if (startTime != null && maxTime != null) (((maxTime - startTime) / 60000).toInt()) else 30
        if (minutes < 50) {
            minutes = 50
        }
        if (minutes % 10 != 0) {
            minutes += (10 - (minutes % 10))
        }

        val maxY = 101

        val ratioX = (this.width - innerPadding - innerPadding) * 1.0 / minutes // 横向比率
        val ratioY = ((this.height - innerPadding - innerPadding) * 1.0 / maxY).toFloat() // 纵向比率
        val stratY = height - innerPadding

        val pathFilterAlpha = Path()
        var isFirstPoint = true

        val textSize = dpSize * 8.5f
        paint.textSize = textSize

        paint.textAlign = Paint.Align.CENTER

        val columnSize = if (minutes <= 90) {
            10
        } else if (minutes <= 120) {
            15
        } else if (minutes <= 180) {
            20
        } else {
            30
        }

        for (point in 0..minutes) {
            if (point % 5 == 0) {
                if (point % columnSize == 0) {
                    paint.color = Color.parseColor("#888888")
                    val text = (point).toString() + "分"
                    canvas.drawText(
                            text,
                            (point * ratioX).toInt() + innerPadding,
                            this.height - innerPadding + textSize + (dpSize * 2),
                            paint
                    )
                    canvas.drawCircle(
                            (point * ratioX).toInt() + innerPadding,
                            this.height - innerPadding,
                            potintRadius,
                            paint
                    )
                    paint.strokeWidth = if (point == 0) potintRadius else 2f
                    if (point == 0) {
                        paint.color = Color.parseColor("#888888")
                    } else {
                        paint.color = Color.parseColor("#aa888888")
                    }
                    canvas.drawLine(
                            (point * ratioX).toInt() + innerPadding, innerPadding,
                            (point * ratioX).toInt() + innerPadding, this.height - innerPadding, paint
                    )
                } else {
                    paint.strokeWidth = 2f
                    paint.color = Color.parseColor("#aa888888")
                    canvas.drawLine(
                            (point * ratioX).toInt() + innerPadding, innerPadding,
                            (point * ratioX).toInt() + innerPadding, this.height - innerPadding, paint
                    )
                }
            }
        }

        paint.textAlign = Paint.Align.RIGHT
        for (point in 0..maxY) {
            paint.color = Color.parseColor("#888888")
            if (point % 10 == 0) {
                if (point > 0) {
                    canvas.drawText(
                            point.toString() + "%",
                            innerPadding - dpSize * 4,
                            innerPadding + ((maxY - point) * ratioY).toInt() + textSize / 2.2f,
                            paint
                    )
                    canvas.drawCircle(
                            innerPadding,
                            innerPadding + ((maxY - point) * ratioY).toInt(),
                            potintRadius,
                            paint
                    )
                }
                paint.strokeWidth = if (point == 0) potintRadius else 2f
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

        paint.color = getColorAccent()
        if (startTime != null) {
            val first = samples.first()
            val last = samples.last()
            val startX = ((first.startTime - startTime) / 60000f * ratioX).toFloat() + innerPadding
            val endX = ((last.endTime - startTime) / 60000f * ratioX).toFloat() + innerPadding
            pathFilterAlpha.moveTo(startX, stratY - (first.capacity * ratioY))
            for (sample in samples) {
                val currentX = ((sample.startTime - startTime) / 60000f * ratioX).toFloat() + innerPadding
                pathFilterAlpha.lineTo(currentX, stratY - (sample.capacity * ratioY))
            }
            pathFilterAlpha.lineTo(endX, stratY - (last.capacity * ratioY))
        }

        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f

        paint.color = Color.parseColor("#8BC34A")
        canvas.drawPath(pathFilterAlpha, paint)

        // paint.textSize = dpSize * 12f
        // paint.textAlign = Paint.Align.RIGHT
        // paint.style = Paint.Style.FILL
        // canvas.drawText("电量/时间", width - innerPadding, innerPadding - (dpSize * 4f), paint)
    }
}
