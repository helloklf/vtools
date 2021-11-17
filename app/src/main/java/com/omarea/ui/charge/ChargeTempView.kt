package com.omarea.ui.charge

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.omarea.store.ChargeSpeedStore
import com.omarea.vtools.R

class ChargeTempView : View {
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val samples = storage.temperature
        samples.sortBy { it.capacity }
        // Toast.makeText(context, "" + samples.map { "" + it.capacity + ":" + it.temperature }.joinToString(",    "), Toast.LENGTH_SHORT).show()

        val potintRadius = 12f
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

        val minY = 30
        val maxY = if (maxTemperature != null && maxTemperature > 50) (maxTemperature.toInt() + 2) else 51

        val ratioX = (this.width - innerPadding - yAxisWidth) * 1.0 / 100 // 横向比率
        val ratioY = ((this.height - innerPadding - innerPadding) * 1.0 / (maxY - minY)).toFloat() // 纵向比率
        val startY = height - innerPadding

        paint.textAlign = Paint.Align.CENTER
        paint.strokeWidth = 1f
        for (point in 0..101) {
            if (point % 20 == 0) {
                paint.color = Color.parseColor("#888888")
                val text = (point).toString() + "%"
                canvas.drawText(
                    text,
                    (point * ratioX).toInt() + yAxisWidth,
                    this.height - innerPadding + textSize + (dpSize * 2),
                    paint
                )
            }
            if (point % 10 == 0) {
                paint.color = Color.parseColor("#40888888")
                canvas.drawLine(
                        (point * ratioX).toInt() + yAxisWidth, innerPadding,
                        (point * ratioX).toInt() + yAxisWidth, this.height - innerPadding, paint
                )
            }
        }

        paint.textAlign = Paint.Align.LEFT
        paint.strokeWidth = 2f
        for (point in 0..(maxY - minY)) {
            val y = innerPadding + ((maxY - minY - point) * ratioY).toInt()
            if (point % 5 == 0) {
                if (point > 0) {
                    paint.color = Color.parseColor("#888888")
                    canvas.drawText(
                        "${point+minY}°C",
                        0f,
                        y + textSize / 2f,
                        paint
                    )
                }
                paint.pathEffect = null
            } else {
                paint.pathEffect = dashPathEffect
            }
            paint.color = Color.parseColor("#40888888")
            canvas.drawLine(
                yAxisWidth,
                y,
                (this.width - innerPadding),
                y,
                paint
            )
        }

        paint.pathEffect = null
        paint.color = Color.parseColor("#801474e4")
        for (sample in samples) {
            val pointX = (sample.capacity * ratioX).toFloat() + innerPadding
            val temperature:Float = if (sample.temperature > minY) (sample.temperature - minY) else 0f

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
        paint.strokeWidth = 8f

        paint.color = Color.parseColor("#1474e4")
        canvas.drawPath(pathFilterAlpha, paint)

        // paint.textSize = dpSize * 12f
        // paint.textAlign = Paint.Align.RIGHT
        // paint.style = Paint.Style.FILL
        // canvas.drawText("温度/电量", width - innerPadding, innerPadding - (dpSize * 4f), paint)
    }
}
