package com.omarea.common.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.omarea.common.R

class ProgressCircle : View {
    //-------------必须给的数据相关-------------
    private val str = arrayOf("已用", "可用")
    private var ratio = 0
    private var ratioState = 0

    //圆的直径
    private var mRadius = 300f

    //圆的粗细
    private var mStrokeWidth = 10f

    //文字大小
    private var textSize = 20

    //-------------画笔相关-------------
    //圆环的画笔
    private var cyclePaint: Paint? = null

    //文字的画笔
    private var textPaint: Paint? = null

    //标注的画笔
    private var labelPaint: Paint? = null

    //-------------颜色相关-------------
    //边框颜色和标注颜色
    private val mColor = intArrayOf(-0xec712a, 0x55888888, -0x1a8c8d, -0xb03c09, -0xe8a, -0x7e387c)

    // private int[] mColor = new int[]{0xFFF06292, 0xFF9575CD, 0xFFE57373, 0xFF4FC3F7, 0xFFFFF176, 0xFF81C784};
    //文字颜色
    private val textColor = -0x777778

    //-------------View相关-------------
    //View自身的宽和高
    private var mHeight: Int = 0
    private var mWidth: Int = 0

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        @SuppressLint("CustomViewStyleable")
        val array = context.obtainStyledAttributes(attrs, R.styleable.ProgressState)
        val total = array.getInteger(R.styleable.ProgressState_total, 1)
        val current = array.getInteger(R.styleable.ProgressState_current, 1)
        ratio = (current * 100.0 / total).toInt()
        //strPercent = new int[]{100 - feeRatio, feeRatio};
        array.recycle()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        @SuppressLint("CustomViewStyleable")
        val array = context.obtainStyledAttributes(attrs, R.styleable.ProgressState)
        val total = array.getInteger(R.styleable.ProgressState_total, 1)
        val current = array.getInteger(R.styleable.ProgressState_current, 1)
        ratio = (current * 100.0 / total).toInt()
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
        val mStrokeWidth = dp2px(context, 10f)
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

    private var temperature = 35F
    fun setData(total: Float, fee: Float, temperature: Float) {
        if (fee == total && total == 0F) {
            ratio = 0
        } else {
            val feeRatio = (fee * 100.0 / total).toInt()
            ratio = 100 - feeRatio
        }
        this.temperature = temperature
        // 动画更新
        // cgangePer(ratio)
        // 无动画更新
        ratioState = ratio
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
        labelPaint!!.strokeWidth = 20f
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
        // cyclePaint!!.alpha = 128
        canvas.drawArc(RectF(0f, 0f, mRadius, mRadius), 0f, 360f, false, cyclePaint!!)
        /*
        if (ratio == 0) {
            return
        }
        */
        // cyclePaint!!.alpha = 255


        if (temperature >= 48 || ratioState < 11) {
            cyclePaint!!.color = Color.rgb(255, 15, 0)
        } else if (temperature > 44 || ratio < 16) {
            cyclePaint!!.color = Color.RED //resources.getColor(R.color.color_load_low)
        }

        /*
        val dashPathEffect = DashPathEffect(floatArrayOf(15 / 3f, 15 * 2 / 3f), 0f)

        val mSweepGradient = SweepGradient(
            canvas.getWidth() / 2f,
            canvas.getHeight() / 2f, //以圆弧中心作为扫描渲染的中心以便实现需要的效果
            intArrayOf(
                    resources.getColor(R.color.color_load_low),
                    resources.getColor(R.color.color_load_mid),
                    resources.getColor(R.color.color_load_hight),
                    resources.getColor(R.color.color_load_veryhight)
            ),
            floatArrayOf(0f, 0.33f, 0.67f, 1f)
        );
        val matrix = Matrix()
        matrix.setRotate(-108f, canvas.width / 2f, canvas.height / 2f)
        mSweepGradient.setLocalMatrix(matrix)

        cyclePaint!!.setShader(mSweepGradient)
        cyclePaint!!.setPathEffect(dashPathEffect);
        */

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