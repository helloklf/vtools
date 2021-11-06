package com.omarea.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.omarea.vtools.R

class CpuChartView : View {
    private var ratio = 0
    private var ratioState = 0

    //圆的直径
    private var mRadius = 300f

    //圆的粗细
    private var mStrokeWidth = 40f

    //文字大小
    private var textSize = 20

    //-------------画笔相关-------------
    //圆环的画笔
    private var cyclePaint: Paint? = null

    //文字颜色
    private val textColor = -0x777778

    //-------------View相关-------------
    //View自身的宽和高
    private var mHeight: Int = 0
    private var mWidth: Int = 0

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
        @SuppressLint("CustomViewStyleable")
        val array = context.obtainStyledAttributes(attrs, R.styleable.RamInfo)
        val total = array.getInteger(R.styleable.RamInfo_total, 1)
        val fee = array.getInteger(R.styleable.RamInfo_free, 1)
        val feeRatio = (fee * 100.0 / total).toInt()
        ratio = 100 - feeRatio
        //strPercent = new int[]{100 - feeRatio, feeRatio};
        array.recycle()
        getColorAccent()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        @SuppressLint("CustomViewStyleable") val array = context.obtainStyledAttributes(attrs, R.styleable.RamInfo)
        val total = array.getInteger(R.styleable.RamInfo_total, 1)
        val fee = array.getInteger(R.styleable.RamInfo_free, 1)
        val feeRatio = (fee * 100.0 / total).toInt()
        ratio = feeRatio
        //strPercent = new int[]{100 - feeRatio, feeRatio};
        array.recycle()
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
        mWidth = w
        mHeight = h
        val mStrokeWidth = w / 7
        this.mStrokeWidth = mStrokeWidth.toFloat()
        this.textSize = dp2px(context, 18f)
        if (w > h) {
            this.mRadius = (h * 0.9 - mStrokeWidth).toInt().toFloat()
        } else {
            this.mRadius = (w * 0.9 - mStrokeWidth).toInt().toFloat()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //移动画布到圆环的左上角
        canvas.translate(mWidth / 2 - mRadius / 2, mHeight / 2 - mRadius / 2)
        //初始化画笔
        initPaint()
        //画圆环
        drawCycle(canvas)
    }

    fun setData(total: Float, fee: Float) {
        if (fee == total && total == 0F) {
            ratio = 0
        } else {
            val feeRatio = (fee * 100.0 / total).toInt()
            ratio = 100 - feeRatio
        }

        if (Math.abs(ratio - ratioState) > 10) {
            cgangePer(ratio) // animationTo
        } else {
            ratioState = ratio
            invalidate()
        }
    }

    /**
     * 初始化画笔
     */
    private fun initPaint() {
        //边框画笔
        cyclePaint = Paint()
        cyclePaint!!.isAntiAlias = true
        cyclePaint!!.style = Paint.Style.STROKE
        cyclePaint!!.strokeWidth = mStrokeWidth
    }

    fun cgangePer(per: Int) {
        val perOld = this.ratioState
        val va = ValueAnimator.ofInt(perOld, per)
        va.duration = 200
        va.interpolator = DecelerateInterpolator()
        va.addUpdateListener { animation ->
            ratioState = animation.animatedValue as Int
            invalidate()
        }
        va.start()

    }

    /**
     * 画圆环
     * @param canvas
     */
    private fun drawCycle(canvas: Canvas) {
        cyclePaint!!.color = 0x22888888
        canvas.drawArc(RectF(0f, 0f, mRadius, mRadius), 0f, 360f, false, cyclePaint!!)
        if (ratio > 85) {
            cyclePaint!!.color = resources.getColor(R.color.color_load_veryhight)
        } else if (ratio > 65) {
            cyclePaint!!.color = resources.getColor(R.color.color_load_hight)
        } else {
            cyclePaint!!.color = accentColor
        }
        if (ratio > 50) {
            cyclePaint?.alpha = 255
        } else {
            cyclePaint?.alpha = 127 + ((ratio / 100.0f) * 255).toInt()
        }

        cyclePaint!!.setStrokeCap(Paint.Cap.ROUND)
        if (ratio < 1 && (ratioState <= 2)) {
            return
        } else if (ratioState >= 98) {
            canvas.drawArc(RectF(0f, 0f, mRadius, mRadius), -90f, 360f, false, cyclePaint!!)
        } else {
            canvas.drawArc(RectF(0f, 0f, mRadius, mRadius), -90f, (ratioState * 3.6f), false, cyclePaint!!)
        }
    }
}