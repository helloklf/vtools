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

class FpsDataView : View {
    private lateinit var storage: FpsWatchStore
    enum class DIMENSION {
        TEMPERATURE, // 温度
        LOAD, // 负载
        CAPACITY, // 电量
    }

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

    private fun minutes2Str(minutes: Double): String {
        if (minutes >= 1140) {
            return "" + (minutes / 1140) + "d" + ((minutes % 1140) / 60) + "h"
        } else if (minutes > 60) {
            return "" + (minutes / 60) + "h" + (minutes % 60) + "m"
        } else if (minutes == 0.0) {
            return "0"
        } else if (minutes >= 1) {
            return "" + (minutes).toInt() + "m" + (minutes % 1 * 60).toInt().toString() + "s"
        } else {
            return (minutes * 60).toInt().toString() + "s"
        }
    }

    private val paint = Paint()
    private val dashPathEffect = DashPathEffect(floatArrayOf(4f, 8f), 0f)
    private var rightDimension = DIMENSION.values().first()
    private var sessionId:Long = 0L

    public fun setSessionId(sessionId: Long) {
        if (this.sessionId != sessionId) {
            this.sessionId = sessionId
            invalidate()
        }
    }

    public fun getSessionId(): Long {
        return this.sessionId
    }

    public fun setRightDimension(rightDIMENSION: DIMENSION) {
        if (this.rightDimension != rightDIMENSION) {
            this.rightDimension = rightDIMENSION
            invalidate()
        }
    }

    public fun getRightDimension(): DIMENSION {
        return this.rightDimension
    }

    private fun drawLeft(canvas: Canvas) {
        val samples = storage.sessionFpsData(this.sessionId)
        if (samples.size < 1) {
            return
        }

        paint.reset()
        val pointRadius = 4f
        paint.strokeWidth = 2f

        val dpSize = dp2px(this.context, 1f)
        val innerPadding = dpSize * 18f
        val paddingTop = dpSize * 4f

        val minutes = samples.size / 60.0

        val maxValue = samples.maxOrNull()!!
        val maxValueInt = maxValue.toInt() + (if (maxValue % 1 == 0f) 1 else 0)
        val maxY = when {
            maxValueInt > 160 -> maxValueInt
            maxValueInt > 144 -> 160
            maxValueInt > 120 -> 144
            maxValueInt > 90 -> 120
            maxValueInt > 60 -> 90
            else -> 60
        }

        val ratioX = (this.width - innerPadding - innerPadding) * 1.0 / minutes // 横向比率
        val ratioY = ((this.height - innerPadding - paddingTop) * 1.0 / maxY).toFloat() // 纵向比率
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
            val text = minutes2Str(point * scaleX)
            canvas.drawText(
                    text,
                    drawX,
                    this.height - innerPadding + textSize + (dpSize * 2),
                    paint
            )
            paint.color = Color.parseColor("#40888888")
            canvas.drawLine(
                    drawX, paddingTop,
                    drawX, this.height - innerPadding, paint
            )
        }

        paint.strokeWidth = 2f
        paint.pathEffect = dashPathEffect
        paint.textAlign = Paint.Align.RIGHT
        val keyValue = when {
            maxValueInt > 160 -> arrayListOf(0, 30, 60, 90, 120, maxValueInt)
            maxValueInt > 144 -> arrayListOf(0, 30, 60, 90, 120, 160)
            maxValueInt > 120 -> arrayListOf(0, 30, 60, 90, 120, 144)
            maxValueInt > 90 -> arrayListOf(0, 30, 60, 90, 120)
            maxValueInt > 60 -> arrayListOf(0, 30, 60, 90)
            else -> arrayListOf(0, 20, 40, 60)
        }
        for (point in 0..maxY) {
            paint.color = Color.parseColor("#888888")
            if (keyValue.contains(point)) {
                if (point > 0) {
                    canvas.drawText(
                            point.toString(),
                            innerPadding - dpSize * 4,
                            paddingTop + ((maxY - point) * ratioY).toInt() + textSize / 2.2f,
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
                        paddingTop + ((maxY - point) * ratioY).toInt(),
                        (this.width - innerPadding),
                        paddingTop + ((maxY - point) * ratioY).toInt(),
                        paint
                )
            }
        }

        paint.reset()
        paint.color = getColorAccent()
        val last = samples.last()
        val first = samples.first()
        val startX = innerPadding
        val endX = ((samples.size / 60f) * ratioX).toFloat() + innerPadding
        var lastX = startX
        var lastY = startY - (first * ratioY)

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

    private fun drawDimensionTemperature(canvas: Canvas) {
        val samples = storage.sessionTemperatureData(this.sessionId)
        if (samples.size < 1) {
            return
        }

        paint.reset()
        val pointRadius = 4f
        paint.strokeWidth = 2f

        val dpSize = dp2px(this.context, 1f)
        val innerPadding = dpSize * 18f
        val paddingTop = dpSize * 4f

        val minutes = samples.size / 60.0

        val maxValue = samples.maxOrNull()!!
        val maxValueInt = maxValue.toInt() + (if (maxValue % 1 == 0f) 1 else 0)
        val maxY = when {
            maxValueInt > 60 -> maxValueInt
            maxValueInt > 50 -> 55
            maxValueInt > 45 -> 50
            else -> 45
        }

        val width = this.width
        val ratioX = (this.width - innerPadding - innerPadding) * 1.0 / minutes // 横向比率
        val ratioY = ((this.height - innerPadding - paddingTop) * 1.0 / maxY).toFloat() // 纵向比率
        val startY = height - innerPadding

        val textSize = dpSize * 8.5f
        paint.textSize = textSize

        paint.strokeWidth = 2f
        paint.pathEffect = dashPathEffect
        paint.textAlign = Paint.Align.LEFT
        val keyValue = arrayListOf(35, 40, 45, 50, 55, 60)
        paint.color = Color.parseColor("#4087d3ff")
        for (point in 0..maxY) {
            if (keyValue.contains(point)) {
                paint.color = Color.parseColor("#808080")
                if (point > 0) {
                    canvas.drawText(
                            if (point == maxY) { point.toString() } else point.toString(),
                            width - innerPadding + 8,
                            paddingTop + ((maxY - point) * ratioY).toInt() + textSize / 2.2f,
                            paint
                    )
                }
                if (point != maxY) {
                    paint.strokeWidth = if (point == 0) pointRadius else 2f
                    paint.color = Color.parseColor("#4087d3ff")
                    canvas.drawLine(
                            innerPadding,
                            paddingTop + ((maxY - point) * ratioY).toInt(),
                            (this.width - innerPadding),
                            paddingTop + ((maxY - point) * ratioY).toInt(),
                            paint
                    )
                }
            }
        }

        paint.reset()
        paint.color = getColorAccent()
        val last = samples.last()
        val first = samples.first()
        val startX = innerPadding
        val endX = ((samples.size / 60f) * ratioX).toFloat() + innerPadding
        var lastX = startX
        var lastY = startY - (first * ratioY)

        paint.isAntiAlias = true
        paint.strokeWidth = 8f
        paint.style = Paint.Style.FILL
        var index = 0

        paint.pathEffect = null
        paint.color = Color.parseColor("#8087d3ff")
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

    private fun drawDimensionLoad(canvas: Canvas) {
        val samplesCpu = storage.sessionCpuLoadData(this.sessionId)
        val samplesGpu = storage.sessionGpuLoadData(this.sessionId)
        if (samplesCpu.size < 1 || samplesGpu.size < 1) {
            return
        }

        paint.reset()
        val pointRadius = 4f
        paint.strokeWidth = 2f

        val dpSize = dp2px(this.context, 1f)
        val innerPadding = dpSize * 18f
        val paddingTop = dpSize * 4f

        val minutes = samplesCpu.size / 60.0

        val maxY = 100

        val width = this.width
        val ratioX = (this.width - innerPadding - innerPadding) * 1.0 / minutes // 横向比率
        val ratioY = ((this.height - innerPadding - paddingTop) * 1.0 / maxY).toFloat() // 纵向比率
        val startY = height - innerPadding

        val textSize = dpSize * 8.5f
        paint.textSize = textSize

        paint.strokeWidth = 2f
        paint.pathEffect = dashPathEffect
        paint.textAlign = Paint.Align.LEFT
        val keyValue = arrayListOf(50, 75, 90, 100)
        paint.color = Color.parseColor("#4087d3ff")
        for (point in 0..maxY) {
            if (keyValue.contains(point)) {
                paint.color = Color.parseColor("#808080")
                if (point > 0) {
                    canvas.drawText(
                            if (point == maxY) { point.toString() } else point.toString(),
                            width - innerPadding + 8,
                            paddingTop + ((maxY - point) * ratioY).toInt() + textSize / 2.2f,
                            paint
                    )
                }
                if (point != maxY) {
                    paint.strokeWidth = if (point == 0) pointRadius else 2f
                    paint.color = Color.parseColor("#4087d3ff")
                    canvas.drawLine(
                            innerPadding,
                            paddingTop + ((maxY - point) * ratioY).toInt(),
                            (this.width - innerPadding),
                            paddingTop + ((maxY - point) * ratioY).toInt(),
                            paint
                    )
                }
            }
        }

        paint.reset()
        paint.color = getColorAccent()
        samplesCpu.run {
            val last = last()
            val first = first()
            val startX = innerPadding
            val endX = ((size / 60f) * ratioX).toFloat() + innerPadding
            var lastX = startX
            var lastY = startY - (first * ratioY)

            paint.isAntiAlias = true
            paint.strokeWidth = 8f
            paint.style = Paint.Style.FILL
            var index = 0

            paint.pathEffect = null
            paint.color = Color.parseColor("#80fc6bc5")
            for (sample in this) {
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
        samplesGpu.run {
            val last = last()
            val first = first()
            val startX = innerPadding
            val endX = ((size / 60f) * ratioX).toFloat() + innerPadding
            var lastX = startX
            var lastY = startY - (first * ratioY)

            paint.isAntiAlias = true
            paint.strokeWidth = 8f
            paint.style = Paint.Style.FILL
            var index = 0

            paint.pathEffect = null
            paint.color = Color.parseColor("#8087d3ff")
            for (sample in this) {
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

    private fun drawDimensionCapacity(canvas: Canvas) {
        val samples = storage.sessionCapacityData(this.sessionId)
        if (samples.size < 1) {
            return
        }

        paint.reset()
        val pointRadius = 4f
        paint.strokeWidth = 2f

        val dpSize = dp2px(this.context, 1f)
        val innerPadding = dpSize * 18f
        val paddingTop = dpSize * 4f

        val minutes = samples.size / 60.0

        val maxY = 100

        val width = this.width
        val ratioX = (this.width - innerPadding - innerPadding) * 1.0 / minutes // 横向比率
        val ratioY = ((this.height - innerPadding - paddingTop) * 1.0 / maxY).toFloat() // 纵向比率
        val startY = height - innerPadding

        val textSize = dpSize * 8.5f
        paint.textSize = textSize

        paint.strokeWidth = 2f
        paint.pathEffect = dashPathEffect
        paint.textAlign = Paint.Align.LEFT
        val keyValue = arrayListOf(50, 75, 90, 100)
        paint.color = Color.parseColor("#4087d3ff")
        for (point in 0..maxY) {
            if (keyValue.contains(point)) {
                paint.color = Color.parseColor("#808080")
                if (point > 0) {
                    canvas.drawText(
                            if (point == maxY) { point.toString() } else point.toString(),
                            width - innerPadding + 8,
                            paddingTop + ((maxY - point) * ratioY).toInt() + textSize / 2.2f,
                            paint
                    )
                }
                if (point != maxY) {
                    paint.strokeWidth = if (point == 0) pointRadius else 2f
                    paint.color = Color.parseColor("#4087d3ff")
                    canvas.drawLine(
                            innerPadding,
                            paddingTop + ((maxY - point) * ratioY).toInt(),
                            (this.width - innerPadding),
                            paddingTop + ((maxY - point) * ratioY).toInt(),
                            paint
                    )
                }
            }
        }

        paint.reset()
        paint.color = getColorAccent()
        samples.run {
            val last = last()
            val first = first()
            val startX = innerPadding
            val endX = ((size / 60f) * ratioX).toFloat() + innerPadding
            var lastX = startX
            var lastY = startY - (first * ratioY)

            paint.isAntiAlias = true
            paint.strokeWidth = 8f
            paint.style = Paint.Style.FILL
            var index = 0

            paint.pathEffect = null
            paint.color = Color.parseColor("#8087d3ff")
            for (sample in this) {
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

    private fun drawRight(canvas: Canvas) {
        when (rightDimension) {
            DIMENSION.TEMPERATURE -> drawDimensionTemperature(canvas)
            DIMENSION.CAPACITY -> drawDimensionCapacity(canvas)
            DIMENSION.LOAD -> drawDimensionLoad(canvas)
            else -> {}
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (this.sessionId < 1) {
            return
        }
        drawLeft(canvas)
        drawRight(canvas)
    }
}