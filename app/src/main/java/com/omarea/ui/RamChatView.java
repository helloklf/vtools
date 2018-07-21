package com.omarea.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.omarea.vtools.R;

public class RamChatView extends View {
    //-------------必须给的数据相关-------------
    private String[] str = new String[]{"已用", "可用"};
    //分配比例大小，总比例大小为100,由于经过运算后最后会是99.55左右的数值，导致圆不能够重合，会留出点空白，所以这里的总比例大小我们用101
    private int[] strPercent = new int[]{10, 25};
    //圆的直径
    private float mRadius = 300;
    //圆的粗细
    private float mStrokeWidth = 40;
    //文字大小
    private int textSize = 20;
    //-------------画笔相关-------------
    //圆环的画笔
    private Paint cyclePaint;
    //文字的画笔
    private Paint textPaint;
    //标注的画笔
    private Paint labelPaint;
    //-------------颜色相关-------------
    //边框颜色和标注颜色
    private int[] mColor = new int[]{0xFF138ed6, 0x55888888, 0xFFE57373, 0xFF4FC3F7, 0xFFFFF176, 0xFF81C784};
    // private int[] mColor = new int[]{0xFFF06292, 0xFF9575CD, 0xFFE57373, 0xFF4FC3F7, 0xFFFFF176, 0xFF81C784};
    //文字颜色
    private int textColor = 0xFF888888;
    //-------------View相关-------------
    //View自身的宽和高
    private int mHeight;
    private int mWidth;

    /**
     * dp转换成px
     */
    private int dp2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public RamChatView(Context context) {
        super(context);
    }

    public RamChatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        @SuppressLint("CustomViewStyleable") TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RamInfo);
        int total = array.getInteger(R.styleable.RamInfo_total, 1);
        int fee = array.getInteger(R.styleable.RamInfo_free, 1);
        int feeRatio = (int) (fee * 100.0 / total);
        strPercent = new int[]{100 - feeRatio, feeRatio};
        array.recycle();
    }

    public RamChatView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        @SuppressLint("CustomViewStyleable") TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RamInfo);
        int total = array.getInteger(R.styleable.RamInfo_total, 1);
        int fee = array.getInteger(R.styleable.RamInfo_free, 1);
        int feeRatio = (int) (fee * 100.0 / total);
        strPercent = new int[]{100 - feeRatio, feeRatio};
        array.recycle();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        int mStrokeWidth = dp2px(getContext(), 20);
        this.mStrokeWidth = mStrokeWidth;
        this.textSize = dp2px(getContext(), 18);
        if (w > h) {
            this.mRadius = (int) (h * 0.9 - mStrokeWidth);
        } else {
            this.mRadius = (int) (w * 0.9 - mStrokeWidth);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //移动画布到圆环的左上角
        canvas.translate(mWidth / 2 - mRadius / 2, mHeight / 2 - mRadius / 2);
        //初始化画笔
        initPaint();
        //画圆环
        drawCycle(canvas);
    }

    public void setData(float total, float fee) {
        int feeRatio = (int) (fee * 100.0 / total);
        strPercent = new int[]{100 - feeRatio, feeRatio};
        invalidate();
    }

    /**
     * 初始化画笔
     */
    private void initPaint() {
        //边框画笔
        cyclePaint = new Paint();
        cyclePaint.setAntiAlias(true);
        cyclePaint.setStyle(Paint.Style.STROKE);
        cyclePaint.setStrokeWidth(mStrokeWidth);
        //文字画笔
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(textColor);
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setStrokeWidth(1);
        textPaint.setTextSize(textSize);
        //标注画笔
        labelPaint = new Paint();
        labelPaint.setAntiAlias(true);
        labelPaint.setStyle(Paint.Style.FILL);
        labelPaint.setStrokeWidth(2);
    }

    /**
     * 画圆环
     *
     * @param canvas
     */
    private void drawCycle(Canvas canvas) {
        float startPercent = -90;
        float sweepPercent = 0;
        cyclePaint.setColor(Color.parseColor("#888888"));
        // canvas.drawArc(new RectF(0, 0, mRadius, mRadius), 0, 360, false, cyclePaint);
        for (int i = 0; i < strPercent.length; i++) {
            cyclePaint.setColor(mColor[i]);
            startPercent = sweepPercent + startPercent;
            //这里采用比例占100的百分比乘于360的来计算出占用的角度，使用先乘再除可以算出值
            sweepPercent = strPercent[i] * 360 / 100;
            canvas.drawArc(new RectF(0, 0, mRadius, mRadius), startPercent, sweepPercent + 1, false, cyclePaint);
        }
    }
}