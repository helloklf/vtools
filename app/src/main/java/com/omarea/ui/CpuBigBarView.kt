package com.omarea.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.omarea.vtools.R
import java.util.concurrent.LinkedBlockingQueue


class CpuBigBarView : View {
    private var mainPaint: Paint? = null
    private var mHeight: Float = 0f
    private var mWidth: Float = 0f
    private val maxHistory = 10
    private var loadHisotry = LinkedBlockingQueue<Int>().apply {
        for (i in 0 until maxHistory) {
            add(0)
        }
    };
    private var strokeWidth = 0f
    private var accentColor = 0x22888888

    private fun getColorAccent() {
        val defaultColor = -0x1000000
        val attrsArray = intArrayOf(android.R.attr.colorAccent)
        val typedArray = context.obtainStyledAttributes(attrsArray)
        accentColor = typedArray.getColor(0, defaultColor)
        typedArray.recycle()
    }

    constructor(context: Context) : super(context) {
        getColorAccent()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        getColorAccent()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        getColorAccent()
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
            strokeWidth = this.width.toFloat() / maxHistory
            mainPaint!!.strokeWidth = 0f
        }

        var index = 0
        val barWidth = strokeWidth
        for (ratio in loadHisotry) {
            if (ratio > 85) {
                mainPaint!!.color = resources.getColor(R.color.color_load_veryhight)
            } else if (ratio > 65) {
                mainPaint!!.color = resources.getColor(R.color.color_load_hight)
            } else {
                mainPaint!!.color = accentColor
            }
            mainPaint?.alpha = 35 + ((ratio / 100.0f / 2) * 255).toInt()

            var top = 0f
            if (ratio <= 2) {
                top = mHeight - 10f
            } else if (ratio >= 98) {
                top = 0f
            } else {
                top = (100 - ratio) * mHeight / 100
            }
            canvas.drawRoundRect((barWidth) * index + (barWidth * 0.05f), top, (barWidth) * index + (barWidth * 0.95f), mHeight, 5f, 5f, mainPaint!!)

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
        if (loadHisotry.size > maxHistory) {
            loadHisotry.poll()
        }
        invalidate()
    }
}