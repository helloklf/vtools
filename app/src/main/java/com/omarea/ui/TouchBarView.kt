package com.omarea.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.omarea.vtools.R

public class TouchBarView : View {
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
    private var touchCurrentX = 0F
    private var touchCurrentY = 0F
    private var gestureStartTime = 0L // 手势开始时间（是指滑动到一定距离，认定触摸手势生效的时间）
    private val FLIP_DISTANCE = dp2px(context, 50f) // 触摸灵敏度（滑动多长距离认为是手势）
    private val effectSize = dp2px(context, 15f).toFloat() // 特效大小
    private val effectWidth = effectSize * 6 // 特效大小
    val cushion = effectWidth + effectWidth * 1.4f // 左右两端的缓冲宽度（数值越大则约缓和）
    val longTouchTime = 250L
    private var isTouchDown = false

    private var currentGraphSize = 0f
    private var vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private var vibratorRun = false
    fun touchVibrator() {
        vibrator.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(20, 10), DEFAULT_AMPLITUDE))
            vibrator.vibrate(VibrationEffect.createOneShot(1, 1))
        } else {
            vibrator.vibrate(longArrayOf(20, 10), -1)
        }
    }

    fun setSize(width: Int, height: Int, orientation: Int) {
        val lp = this.layoutParams
        lp.width = width
        lp.height = height
        this.bakWidth = width
        this.bakHeight = height
        this.setLayoutParams(lp)
        this.orientation = orientation
    }

    // 动画（触摸效果）显示期间，将悬浮窗显示调大，以便显示完整的动效
    private fun setSizeOnTouch() {
        if (bakHeight > 0 || bakWidth > 0) {
            val lp = this.layoutParams
            if (orientation == BOTTOM) {
                lp.width = -1
                lp.height = FLIP_DISTANCE
            } else if (orientation == LEFT || orientation == RIGHT) {
                lp.width = FLIP_DISTANCE
                lp.height = this.height + 200

                // 由于调整触摸条的高度，导致touchStartY的相对位置改变，因此 这里也要对touchStartY进行修改
                touchStartY += 200
            }
            this.layoutParams = lp
        }
    }

    // 动画结束后缩小悬浮窗，以免影响正常操作
    private fun resumeBackupSize() {
        val lp = this.layoutParams
        lp.width = bakWidth
        lp.height = bakHeight
        this.layoutParams = lp
    }


    fun cgangePer(per: Float) {
        val perOld = this.currentGraphSize
        val va = ValueAnimator.ofFloat(perOld, per)
        va.duration = 250
        va.interpolator = DecelerateInterpolator()
        va.addUpdateListener { animation ->
            currentGraphSize = animation.animatedValue as Float
            invalidate()
        }
        va.start()
        va.cancel()
    }


    private fun isLongTime(): Boolean {
        return gestureStartTime > 0 && System.currentTimeMillis() - gestureStartTime > longTouchTime
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            Log.d("TouchBarView", "x:" + event.x + "  y:" + event.y)
            when {
                event.action == MotionEvent.ACTION_DOWN -> {
                    isTouchDown = true
                    touchStartX = event.x
                    touchStartY = event.y
                    gestureStartTime = 0
                    currentGraphSize = dp2px(context, 5f).toFloat()
                    setSizeOnTouch()
                    invalidate()
                    // cgangePer(effectSize)
                    currentGraphSize = 10f
                    invalidate()
                    vibratorRun = true
                }
                event.action == MotionEvent.ACTION_MOVE -> {
                    touchCurrentX = event.x
                    touchCurrentY = event.y
                    var a = -1f
                    var b = -1f
                    if (orientation == LEFT) {
                        a = touchCurrentX
                        b = touchStartX
                    }
                    else if (orientation == RIGHT) {
                        a = touchStartX
                        b = touchCurrentX
                    }
                    else if (orientation == BOTTOM) {
                        a = touchStartY
                        b = touchCurrentY
                    }
                    if (a - b > FLIP_DISTANCE) {
                        Log.d("TouchBarView 成功", "x:" + event.x + "  y:" + event.y)
                        currentGraphSize = effectSize
                        if (gestureStartTime < 1) {
                            val currentTime = System.currentTimeMillis()
                            gestureStartTime = currentTime
                            postDelayed({
                                if (currentTime == gestureStartTime) {
                                    invalidate()
                                }
                            }, longTouchTime)
                        } else if (vibratorRun && isLongTime()) {
                            touchVibrator()
                            vibratorRun = false
                        }
                        invalidate()
                    } else {
                        vibratorRun = true
                        gestureStartTime = 0
                        currentGraphSize = (a - b) / FLIP_DISTANCE * effectSize
                        invalidate()
                        Log.d("TouchBarView 移动", "x:" + event.x + "  y:" + event.y)
                    }
                }
                event.action == MotionEvent.ACTION_UP -> {
                    if (!isTouchDown) {
                        return false
                    }
                    isTouchDown = false
                    if (orientation == LEFT) {
                        if (event.y - touchStartY > FLIP_DISTANCE * 2) {
                            // 下滑打开通知栏
                            // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                        } else if (touchStartY - event.y > FLIP_DISTANCE * 2) {
                            // 上滑打开最近任务
                            // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
                        } else if (event.x - touchStartX > FLIP_DISTANCE) {
                            // 向屏幕内侧滑动 - 停顿250ms 打开最近任务，不停顿则“返回”
                            if (isLongTime()) {
                                // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
                                performLongClick()
                            } else {
                                // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_BACK)
                                performClick()
                            }
                        }
                    }
                    else if (orientation == RIGHT) {
                        if (event.y - touchStartY > FLIP_DISTANCE * 2) {
                            // 下滑打开通知栏
                            // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                        }
                        else if (touchStartY - event.y > FLIP_DISTANCE * 2) {
                            // 上滑打开最近任务
                            // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
                        }
                        else if (touchStartX - event.x > FLIP_DISTANCE) {
                            // 向屏幕内侧滑动 - 停顿250ms 打开最近任务，不停顿则“返回”
                            if (isLongTime()) {
                                // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
                                performLongClick()
                            } else {
                                // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_BACK)
                                performClick()
                            }
                        }
                    }
                    else if (orientation == BOTTOM) {
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
        return true
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
        currentGraphSize = 0f
        gestureStartTime = 0
        // resumeBackupSize()
        invalidate()
    }

    /**
     * 计算手势中的图标显示位置
     */
    private fun getEffectIconRectF(centerX: Float, centerY: Float): RectF {
        val iconRadius = 20f
        return RectF(centerX - iconRadius, centerY - iconRadius, centerX + iconRadius, centerY + iconRadius)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isTouchDown) {
            resumeBackupSize()
            return
        }

        if (currentGraphSize < 10) {
            return
        }

        val p = Paint()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.color = 0x101010
        p.alpha = 240

        // 纵向触摸条
        if (orientation == LEFT) {
            if (touchStartY > 0) {
                val path = Path()
                val graphHeight = -currentGraphSize
                val graphWidth = effectWidth
                val centerX = -graphHeight
                val centerY = touchStartY // height / 2

                path.moveTo((centerX + graphHeight), (centerY - cushion))
                path.quadTo((centerX + graphHeight * 2), (centerY - graphWidth), centerX, (centerY - graphWidth / 2)) // 左侧平缓弧线
                path.quadTo((centerX - graphHeight * 2.4f), centerY, centerX, (centerY + graphWidth / 2)) // 顶部圆拱
                path.quadTo((centerX + graphHeight * 2), (centerY + graphWidth), (centerX + graphHeight), (centerY + cushion)) // 右侧平缓弧线

                canvas.drawPath(path, p)

                if (touchCurrentX - touchStartX > FLIP_DISTANCE) {
                    canvas.drawBitmap(
                            BitmapFactory.decodeResource(context.getResources(), if (isLongTime()) R.drawable.touch_tasks else R.drawable.touch_arrow_right),
                            null,
                            getEffectIconRectF(centerX, centerY),
                            p)
                }
            }
        } else if (orientation == RIGHT) {
            if (touchStartY > 0) {
                val path = Path()
                val graphHeight = currentGraphSize
                val graphWidth = effectWidth
                val centerX = width - graphHeight
                val centerY = touchStartY // height / 2

                path.moveTo((centerX + graphHeight), (centerY - cushion))
                path.quadTo((centerX + graphHeight * 2), (centerY - graphWidth), centerX, (centerY - graphWidth / 2)) // 左侧平缓弧线
                path.quadTo((centerX - graphHeight * 2.4f), centerY, centerX, (centerY + graphWidth / 2)) // 顶部圆拱
                path.quadTo((centerX + graphHeight * 2), (centerY + graphWidth), (centerX + graphHeight), (centerY + cushion)) // 右侧平缓弧线

                canvas.drawPath(path, p)

                if (touchStartX - touchCurrentX > FLIP_DISTANCE) {
                    canvas.drawBitmap(
                            BitmapFactory.decodeResource(context.getResources(), if (isLongTime()) R.drawable.touch_tasks else R.drawable.touch_arrow_left),
                            null,
                            getEffectIconRectF(centerX, centerY),
                            p)
                }
            }
        }
        // 横向触摸条
        else {
            if (touchStartX > 0) {
                val path = Path()
                val graphHeight = currentGraphSize // 35
                val graphWidth = effectWidth
                val centerX = touchStartX // width / 2
                val centerY = height - graphHeight

                path.moveTo((centerX - cushion), (centerY + graphHeight)) //贝赛尔的起始点moveTo(x,y)
                path.quadTo((centerX - graphWidth), (centerY + graphHeight * 2), (centerX - graphWidth / 2), centerY) // 左侧平缓弧线
                path.quadTo(centerX, (centerY - graphHeight * 2.5f), (centerX + graphWidth / 2), centerY) // 顶部圆拱
                path.quadTo((centerX + graphWidth), (centerY + graphHeight * 2), (centerX + cushion), (centerY + graphHeight)) // 右侧平缓弧线

                canvas.drawPath(path, p)

                if (touchStartY - touchCurrentY > FLIP_DISTANCE) {
                    canvas.drawBitmap(
                            BitmapFactory.decodeResource(context.getResources(), if (isLongTime()) R.drawable.touch_tasks else R.drawable.touch_home),
                            null,
                            getEffectIconRectF(centerX, centerY),
                            p)
                }
            }
        }
    }
}
