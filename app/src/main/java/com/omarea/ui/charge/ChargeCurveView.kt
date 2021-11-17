package com.omarea.ui.charge

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.omarea.store.ChargeSpeedStore
import com.omarea.vtools.R

class ChargeCurveView : View {
    private lateinit var storage: ChargeSpeedStore
    private val dashPathEffect = DashPathEffect(floatArrayOf(4f, 8f), 0f)

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
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
        val samples = storage.statistics()

        val potintRadius = 12f
        val paint = Paint()
        paint.strokeWidth = 2f

        val dpSize = dp2px(this.context, 1f)
        val innerPadding = dpSize * 24f

        val maxIO = samples.map { it.io }.max()
        var maxAmpere = if (maxIO != null) (maxIO / 1000 + 1) else 10
        if (maxAmpere < 3) {
            maxAmpere = 3
        } else if (maxAmpere < 6) {
            maxAmpere = 6
        }
        val yScale = when {
            maxAmpere < 7 -> 2
            else -> 1
        }

        val ratioX = (this.width - innerPadding - innerPadding) * 1.0 / 100 // 横向比率
        val ratioY = ((this.height - innerPadding - innerPadding) * 1.0 / maxAmpere).toFloat() // 纵向比率
        val stratY = height - innerPadding

        val pathFilterAlpha = Path()
        var isFirstPoint = true

        val textSize = dpSize * 8.5f
        paint.textSize = textSize

        paint.textAlign = Paint.Align.CENTER
        for (point in 0..101) {
            if (point % 20 == 0) {
                paint.color = Color.parseColor("#888888")
                val text = (point).toString() + "%"
                canvas.drawText(
                        text,
                        (point * ratioX).toInt() + innerPadding,
                        this.height - innerPadding + textSize + (dpSize * 2),
                        paint
                )
                paint.strokeWidth = 2f
            } else {
                paint.strokeWidth = 1f
            }
            if (point % 10 == 0) {
                paint.color = Color.parseColor("#40888888")
                canvas.drawLine(
                        (point * ratioX).toInt() + innerPadding, innerPadding,
                        (point * ratioX).toInt() + innerPadding, this.height - innerPadding, paint)
            }
        }

        paint.textAlign = Paint.Align.RIGHT

        val yPoints = yScale * maxAmpere
        paint.pathEffect = dashPathEffect
        for (point in 0..yPoints) {
            val valueY = (point / 1.0 / yScale)
            paint.color = Color.parseColor("#888888")
            if (point > 0 && point % 2 == 0L) {
                canvas.drawText(
                        valueY.toString() + "A",
                        innerPadding - dpSize * 4,
                        innerPadding + ((maxAmpere - valueY) * ratioY).toInt() + textSize / 2.2f,
                        paint
                )
            }
            paint.strokeWidth = if (point == 0L) 2f else 1f
            paint.color = Color.parseColor("#aa888888")
            canvas.drawLine(
                    innerPadding, innerPadding + ((maxAmpere - valueY) * ratioY).toInt(),
                    (this.width - innerPadding), innerPadding + ((maxAmpere - valueY) * ratioY).toInt(), paint)
        }

        paint.pathEffect = null
        paint.color = Color.parseColor("#801474e4")
        for (sample in samples) {
            val pointX = (sample.capacity * ratioX).toFloat() + innerPadding
            val io = if (sample.io < 0) 0F else { sample.io / 1000F } // mA -> A

            if (isFirstPoint) {
                pathFilterAlpha.moveTo(pointX, stratY - (io * ratioY))
                isFirstPoint = false
                canvas.drawCircle(pointX, stratY - (io * ratioY), potintRadius, paint)
            } else {
                pathFilterAlpha.lineTo(pointX, stratY - (io * ratioY))
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
        // canvas.drawText("电池电流/电量", width - innerPadding, innerPadding - (dpSize * 4f), paint)
    }
}
