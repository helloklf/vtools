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

class ChargeTempView : View {
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
        val samples = storage.temperature
        samples.sortBy { it.capacity }
        // Toast.makeText(context, "" + samples.map { "" + it.capacity + ":" + it.temperature }.joinToString(",    "), Toast.LENGTH_SHORT).show()

        val potintRadius = 4f
        val paint = Paint()
        paint.strokeWidth = 2f

        val dpSize = dp2px(this.context, 1f)

        val pathFilterAlpha = Path()
        var isFirstPoint = true

        val textSize = dpSize * 8.5f
        paint.textSize = textSize

        val minTemperature = samples.map { it.temperature }.min()
        val maxTemperature = samples.map { it.temperature }.max()

        val innerPadding = dpSize * 24f
        val yAxisWidth = paint.measureText(maxTemperature.toString()  + "°C")

        val maxY =
                if (maxTemperature != null && maxTemperature > 50) (maxTemperature.toInt() + 2) else 51

        val ratioX = (this.width - innerPadding - yAxisWidth) * 1.0 / 100 // 横向比率
        val ratioY = ((this.height - innerPadding - innerPadding) * 1.0 / maxY).toFloat() // 纵向比率
        val startY = height - innerPadding

        paint.textAlign = Paint.Align.CENTER
        for (point in 0..101) {
            if (point % 10 == 0) {
                paint.color = Color.parseColor("#888888")
                val text = (point).toString() + "%"
                canvas.drawText(
                        text,
                        (point * ratioX).toInt() + yAxisWidth,
                        this.height - innerPadding + textSize + (dpSize * 2),
                        paint
                )
                canvas.drawCircle(
                        (point * ratioX).toInt() + yAxisWidth,
                        this.height - innerPadding,
                        potintRadius,
                        paint
                )
            }
            if (point % 5 == 0) {
                if (point == 0) {
                    paint.strokeWidth = potintRadius
                    paint.color = Color.parseColor("#888888")
                } else {
                    paint.strokeWidth = 2f
                    paint.color = Color.parseColor("#aa888888")
                }
                canvas.drawLine(
                        (point * ratioX).toInt() + yAxisWidth, innerPadding,
                        (point * ratioX).toInt() + yAxisWidth, this.height - innerPadding, paint
                )
            }
        }

        paint.textAlign = Paint.Align.LEFT
        for (point in 0..maxY) {
            if (point % 5 == 0) {
                if (point > 0) {
                    paint.color = Color.parseColor("#888888")
                    canvas.drawText(
                            point.toString() + "°C",
                            0f,
                            innerPadding + ((maxY - point) * ratioY).toInt() + textSize / 2f,
                            paint
                    )
                    canvas.drawCircle(
                            yAxisWidth,
                            innerPadding + ((maxY - point) * ratioY).toInt(),
                            potintRadius,
                            paint
                    )
                }
                paint.color = Color.parseColor("#aa888888")
            } else {
                paint.color = Color.parseColor("#40888888")
            }
            if (point == 0) {
                paint.strokeWidth = potintRadius
                paint.color = Color.parseColor("#888888")
            } else {
                paint.strokeWidth = 2f
            }
            canvas.drawLine(
                    yAxisWidth,
                    innerPadding + ((maxY - point) * ratioY).toInt(),
                    (this.width - innerPadding),
                    innerPadding + ((maxY - point) * ratioY).toInt(),
                    paint
            )
        }

        paint.color = getColorAccent()
        for (sample in samples) {
            val pointX = (sample.capacity * ratioX).toFloat() + innerPadding
            val temperature = sample.temperature

            if (isFirstPoint) {
                pathFilterAlpha.moveTo(pointX, startY - (temperature * ratioY))
                isFirstPoint = false
                canvas.drawCircle(pointX, startY - (temperature * ratioY), potintRadius, paint)
            } else {
                pathFilterAlpha.lineTo(pointX, startY - (temperature * ratioY))
            }
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
        // canvas.drawText("温度/电量", width - innerPadding, innerPadding - (dpSize * 4f), paint)
    }
}
