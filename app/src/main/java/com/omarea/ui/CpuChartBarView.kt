package com.omarea.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.omarea.vtools.R
import java.util.concurrent.LinkedBlockingQueue

class CpuChartBarView : View {
    private var mainPaint: Paint? = null
    private var mHeight: Float = 0f
    private var mWidth: Float = 0f
    private var loadHisotry = LinkedBlockingQueue<Int>();
    private var strokeWidth = 0f

    constructor(context: Context) : super(context) {
        for (i in 0..4) {
            loadHisotry.put(0)
        }
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        @SuppressLint("CustomViewStyleable") val array = context.obtainStyledAttributes(attrs, R.styleable.RamInfo)
        for (i in 0..4) {
            loadHisotry.put(0)
        }
        array.recycle()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        @SuppressLint("CustomViewStyleable") val array = context.obtainStyledAttributes(attrs, R.styleable.RamInfo)
        for (i in 0..5) {
            loadHisotry.put(0)
        }
        array.recycle()
    }

    /**
     * dp转换成px
     */
    private fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w.toFloat()
        mHeight = h.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mainPaint == null) {
            mainPaint = Paint()
            mainPaint!!.isAntiAlias = true
            mainPaint!!.style = Paint.Style.FILL
            strokeWidth = this.width.toFloat() / 5
            mainPaint!!.strokeWidth = 0f
        }

        var index = 0
        val barWidth = strokeWidth
        for (ratio in loadHisotry) {
            if (ratio > 85) {
                mainPaint!!.color = resources.getColor(R.color.color_load_veryhight)
            } else if (ratio > 65) {
                mainPaint!!.color = resources.getColor(R.color.color_load_hight)
            } else if (ratio > 20) {
                mainPaint!!.color = resources.getColor(R.color.color_load_mid)
            } else if (ratio <= 2) {
                mainPaint!!.color = 0x22888888
            } else {
                mainPaint!!.color = resources.getColor(R.color.color_load_low)
            }
            var top = 0f
            if (ratio <= 2) {
                top = mHeight - 10f
            } else if (ratio >= 98) {
                top = 0f
            } else {
                top = (100 - ratio) * mHeight / 100
            }

            canvas.drawRoundRect((barWidth) * index, top, (barWidth) * index + (barWidth * 0.9f), mHeight, 5f, 5f, mainPaint!!)

            index++
        }
    }

    fun setData(total: Float, fee: Float) {
        if (fee == total && total == 0F) {
            loadHisotry.put(0)
        } else {
            val feeRatio = (fee * 100.0 / total).toInt()
            loadHisotry.put(100 - feeRatio)
        }
        if (loadHisotry.size > 5) {
            loadHisotry.poll()
        }
        invalidate()
    }
}