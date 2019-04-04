package com.omarea.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.omarea.vtools.R

public class TouchBarView: View {
    companion object {
        public val RIGHT = 2
        public val BOTTOM = 0
        public val LEFT = 1
    }
    private var orientation = 0

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        @SuppressLint("CustomViewStyleable")
        val array = context.obtainStyledAttributes(attrs, R.styleable.TouchBarView)
        array.recycle()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        @SuppressLint("CustomViewStyleable")
        val array = context.obtainStyledAttributes(attrs, R.styleable.TouchBarView)
        array.recycle()
    }

    private var bakWidth = 0
    private var bakHeight = 0

    private var touchStartX = 0F
    private var touchStartY = 0F
    private val effectSize = dp2px(context, 12.5f).toFloat()
    private var startTime = 0L // 开始触摸的时间
    private val FLIP_DISTANCE = dp2px(context, 70f)

    private var currentX = 0f

    fun setSize(width: Int, height: Int, orientation: Int) {
        val  lp = this.layoutParams
        lp.width = width
        lp.height = height
        this.bakWidth = width
        this.bakHeight = height
        this.setLayoutParams(lp)
        this.orientation = orientation
    }

    private fun setSizeOnTouch() {
        if (bakHeight > 0 || bakWidth > 0) {
            val  lp = this.layoutParams
            if (orientation == BOTTOM) {
                lp.width = -1
                lp.height = effectSize.toInt() * 2
            } else if (orientation == LEFT || orientation == RIGHT) {
                lp.width = effectSize.toInt() * 2
                lp.height = this.height + 200
            }
            this.layoutParams = lp
        }
    }

    private fun resumeBackupSize () {
        val  lp = this.layoutParams
        lp.width = bakWidth
        lp.height = bakHeight
        this.layoutParams = lp
    }


    fun cgangePer(per: Float) {
        val perOld = this.currentX
        val va = ValueAnimator.ofFloat(perOld, per)
        va.duration = 250
        va.interpolator = DecelerateInterpolator()
        va.addUpdateListener { animation ->
            currentX = animation.animatedValue as Float
            invalidate()
        }
        va.start()
    }


    private fun isLongTime (): Boolean {
        return System.currentTimeMillis() - startTime > 250
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            Log.d("TouchBarView", "x:" + event.x + "  y:" + event.y)
            when {
                event.action == MotionEvent.ACTION_DOWN -> {
                    setSizeOnTouch()
                    touchStartX = event.x
                    touchStartY = event.y
                    startTime = System.currentTimeMillis()
                    currentX = dp2px(context, 5f).toFloat()
                    invalidate()
                    cgangePer(effectSize)
                }
                event.action == MotionEvent.ACTION_MOVE -> {
                    Log.d("TouchBarView 移动", "x:" + event.x + "  y:" + event.y)
                }
                event.action == MotionEvent.ACTION_UP -> {
                    if (orientation == LEFT) {
                        if (event.y - touchStartY > FLIP_DISTANCE * 2) {
                            // 下滑打开通知栏
                            // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                        } else if (touchStartY - event.y > FLIP_DISTANCE * 2) {
                            // 上滑打开最近任务
                            // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
                        } else if (event.x  - touchStartX > FLIP_DISTANCE) {
                            // 向屏幕内侧滑动 - 停顿250ms 打开最近任务，不停顿则“返回”
                            if (isLongTime()) {
                                // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
                                performLongClick()
                            } else {
                                // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_BACK)
                                performClick()
                            }
                        }
                    } else if (orientation == RIGHT) {
                        if (event.y - touchStartY > FLIP_DISTANCE * 2) {
                            // 下滑打开通知栏
                            // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                        } else if (touchStartY - event.y > FLIP_DISTANCE * 2) {
                            // 上滑打开最近任务
                            // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
                        } else if (touchStartX - event.x > FLIP_DISTANCE) {
                            // 向屏幕内侧滑动 - 停顿250ms 打开最近任务，不停顿则“返回”
                            if (isLongTime()) {
                                // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
                                performLongClick()
                            } else {
                                // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_BACK)
                                performClick()
                            }
                        }
                    } else if (orientation == BOTTOM) {
                        if (touchStartY - event.y > FLIP_DISTANCE) {
                            if (isLongTime()) {
                                performLongClick()
                            } else {
                                performClick()
                            }
                        }
                    }
                    cleartEffect()
                }
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * dp转换成px
     */
    private fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 清除手势效果
     */
    private fun cleartEffect() {
        touchStartX = 0f
        touchStartY = 0f
        currentX = 0f
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (this.bakWidth == 0 || this.bakHeight == 0) {
            this.bakWidth = this.width
            this.bakHeight = this.height
        }

        val p = Paint()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.color = 0x000000
        p.alpha = 200

        // 纵向触摸条
        if (orientation == LEFT) {
            if (touchStartY > 0) {
                val path2 = Path()
                path2.moveTo(0f, touchStartY) //设置Path的起点
                path2.quadTo(currentX, touchStartY + 200f, 0f, touchStartY + 400f) //设置贝塞尔曲线的控制点坐标和终点坐标
                canvas.drawPath(path2, p)//画出贝塞尔曲线
            }
            else {
                resumeBackupSize()
            }
        } else if (orientation == RIGHT) {
            if (touchStartY > 0) {
                val path2 = Path()
                path2.moveTo(this.width.toFloat(), touchStartY) //设置Path的起点
                path2.quadTo(this.width - currentX, touchStartY + 200f, this.width.toFloat(), touchStartY + 400f) //设置贝塞尔曲线的控制点坐标和终点坐标
                canvas.drawPath(path2, p)//画出贝塞尔曲线
            }
            else {
                resumeBackupSize()
            }
        }
        // 横向触摸条
        else {
            if (touchStartX > 0) {
                val path = Path()
                val graphHeight = currentX // 35
                val graphWidth = 200
                val centerX = touchStartX // width / 2
                val centerY = height - graphHeight
                val cushion = 500 // 左右两端的缓冲宽度（数值越大则约缓和）

                path.moveTo((centerX - cushion), (centerY + graphHeight)) //贝赛尔的起始点moveTo(x,y)
                path.quadTo((centerX - graphWidth), (centerY + graphHeight * 2), (centerX - graphWidth / 2), centerY) // 左侧平缓弧线
                path.quadTo(centerX, (centerY - graphHeight * 2), (centerX + graphWidth / 2), centerY) // 顶部圆拱
                path.quadTo((centerX + graphWidth), (centerY + graphHeight * 2), (centerX + cushion), (centerY + graphHeight)) // 右侧平缓弧线

                canvas.drawPath(path, p)

                if (currentX > 30) {
                    canvas.drawBitmap(
                        BitmapFactory.decodeResource(context.getResources(), if (isLongTime()) R.drawable.touch_tasks else R.drawable.touch_home),
                        null,
                        RectF(centerX - 30f, 10f, touchStartX + 30f, 70f),
                        Paint())
                }
            } else {
                resumeBackupSize()
            }
        }
    }
}
