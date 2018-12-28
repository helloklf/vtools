package com.omarea.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.omarea.vtools.R


class RamChatView : View {
    //-------------必须给的数据相关-------------
    private val str = arrayOf("已用", "可用")
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
    //文字的画笔
    private var textPaint: Paint? = null
    //标注的画笔
    private var labelPaint: Paint? = null
    // private int[] mColor = new int[]{0xFFF06292, 0xFF9575CD, 0xFFE57373, 0xFF4FC3F7, 0xFFFFF176, 0xFF81C784};
    //文字颜色
    private val textColor = -0x777778
    //-------------View相关-------------
    //View自身的宽和高
    private var mHeight: Int = 0
    private var mWidth: Int = 0

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        @SuppressLint("CustomViewStyleable") val array = context.obtainStyledAttributes(attrs, R.styleable.RamInfo)
        val total = array.getInteger(R.styleable.RamInfo_total, 1)
        val fee = array.getInteger(R.styleable.RamInfo_free, 1)
        val feeRatio = (fee * 100.0 / total).toInt()
        ratio = 100 - feeRatio
        //strPercent = new int[]{100 - feeRatio, feeRatio};
        array.recycle()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        @SuppressLint("CustomViewStyleable") val array = context.obtainStyledAttributes(attrs, R.styleable.RamInfo)
        val total = array.getInteger(R.styleable.RamInfo_total, 1)
        val fee = array.getInteger(R.styleable.RamInfo_free, 1)
        val feeRatio = (fee * 100.0 / total).toInt()
        ratio = feeRatio
        //strPercent = new int[]{100 - feeRatio, feeRatio};
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
        mWidth = w
        mHeight = h
        val mStrokeWidth = dp2px(context, 8f)
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
        invalidate()
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
        //文字画笔
        textPaint = Paint()
        textPaint!!.isAntiAlias = true
        textPaint!!.color = textColor
        textPaint!!.style = Paint.Style.STROKE
        textPaint!!.strokeWidth = 1f
        textPaint!!.textSize = textSize.toFloat()
        //标注画笔
        labelPaint = Paint()
        labelPaint!!.isAntiAlias = true
        labelPaint!!.style = Paint.Style.FILL
        labelPaint!!.strokeWidth = 2f
    }

    /**
     * 画圆环
     * @param canvas
     */
    private fun drawCycle(canvas: Canvas) {
        val startPercent = -90f
        cyclePaint!!.color = 0x44888888 //Color.parseColor("#888888")
        canvas.drawArc(RectF(0f, 0f, mRadius, mRadius), 0f, 360f, false, cyclePaint)
        if (ratio == 0) {
            return
        }
        if (ratioState > 90) {
            cyclePaint!!.color = resources.getColor(R.color.color_load_veryhight)
        } else if (ratioState > 75) {
            cyclePaint!!.color = resources.getColor(R.color.color_load_hight)
        } else if (ratioState > 20) {
            cyclePaint!!.color = resources.getColor(R.color.color_load_mid)
        } else {
            cyclePaint!!.color = resources.getColor(R.color.color_load_low)
        }
        cyclePaint!!.setStrokeCap(Paint.Cap.ROUND)
        canvas.drawArc(RectF(0f, 0f, mRadius, mRadius), -90f, (ratioState * 3.6f) + 1f, false, cyclePaint!!)
        if (ratioState < ratio) {
            ratioState += 1
            invalidate()
        } else if (ratioState > ratio) {
            ratioState -= 1
            invalidate()
        }
    }
}