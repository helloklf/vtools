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
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.omarea.vtools.R

class TouchBarView : View {
    companion object {
        val RIGHT = 2
        val BOTTOM = 0
        val LEFT = 1
    }

    private var orientation = 0

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    private var bakWidth = 0
    private var bakHeight = 0

    private var touchStartX = 0F
    private var touchStartY = 0F
    private var touchCurrentX = 0F
    private var touchCurrentY = 0F

    private var gestureStartTime = 0L // 手势开始时间（是指滑动到一定距离，认定触摸手势生效的时间）
    private var isLongTimeGesture = false
    private val FLIP_DISTANCE = dp2px(context, 50f) // 触摸灵敏度（滑动多长距离认为是手势）
    private val effectSize = dp2px(context, 15f).toFloat() // 特效大小
    private val effectWidth = effectSize * 6 // 特效大小
    val cushion = effectWidth + effectWidth * 1.4f // 左右两端的缓冲宽度（数值越大则约缓和）
    val longTouchTime = 250L
    private var isTouchDown = false
    private var isGestureCompleted = false
    private val iconRadius = dp2px(context, 8f)

    private var currentGraphSize = 0f
    private var vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private var vibratorRun = false
    fun touchVibrator() {
        vibrator.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                lp.height = FLIP_DISTANCE
            } else if (orientation == LEFT || orientation == RIGHT) {
                lp.width = FLIP_DISTANCE
                if (touchStartY < effectWidth) {
                    val toHeight = this.bakHeight + effectWidth.toInt()
                    if (toHeight != lp.height) {
                        lp.height = toHeight
                        touchStartY += effectWidth
                    }
                }
            }
            this.layoutParams = lp
        }
    }

    // 动画结束后缩小悬浮窗，以免影响正常操作
    private fun resumeBackupSize() {
        touchStartX = 0f
        touchStartY = 0f

        val lp = this.layoutParams
        lp.width = bakWidth
        lp.height = bakHeight
        this.layoutParams = lp
    }

    var va: ValueAnimator? = null
    fun cgangePer(per: Float) {
        if (va != null && va!!.isRunning) {
            va!!.cancel()
        }
        va = ValueAnimator.ofFloat(this.currentGraphSize, per)
        va!!.run {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                currentGraphSize = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            when {
                event.action == MotionEvent.ACTION_DOWN -> {
                    isTouchDown = true
                    isGestureCompleted = false
                    touchStartX = event.x
                    touchStartY = event.y
                    gestureStartTime = 0
                    isLongTimeGesture = false
                    currentGraphSize = dp2px(context, 5f).toFloat()
                    setSizeOnTouch()
                    currentGraphSize = 0f
                    // invalidate()
                    // cgangePer(effectSize)
                    vibratorRun = true
                }
                event.action == MotionEvent.ACTION_MOVE -> {
                    if (isGestureCompleted || !isTouchDown) {
                        return true
                    }

                    touchCurrentX = event.x
                    touchCurrentY = event.y
                    var a = -1f
                    var b = -1f
                    if (orientation == LEFT) {
                        a = touchCurrentX
                        b = touchStartX
                    } else if (orientation == RIGHT) {
                        a = touchStartX
                        b = touchCurrentX
                    } else if (orientation == BOTTOM) {
                        a = touchStartY
                        b = touchCurrentY
                    }

                    if (a - b > FLIP_DISTANCE) {
                        currentGraphSize = effectSize
                        if (gestureStartTime < 1) {
                            val currentTime = System.currentTimeMillis()
                            gestureStartTime = currentTime
                            postDelayed({
                                if (isTouchDown && !isGestureCompleted && currentTime == gestureStartTime) {
                                    isLongTimeGesture = true
                                    if (vibratorRun) {
                                        touchVibrator()
                                        vibratorRun = false
                                    }
                                    if (orientation == BOTTOM) {
                                        performLongClick()
                                        isGestureCompleted = true
                                        cleartEffect()
                                    } else {
                                        invalidate()
                                    }
                                }
                            }, longTouchTime)
                        }
                    } else {
                        vibratorRun = true
                        gestureStartTime = 0
                    }
                    var size = (a - b) / FLIP_DISTANCE * effectSize
                    if (size > effectSize) {
                        size = effectSize
                    }
                    currentGraphSize = size
                    invalidate()
                }
                event.action == MotionEvent.ACTION_UP -> {
                    if (!isTouchDown || isGestureCompleted) {
                        return true
                    }

                    isTouchDown = false
                    isGestureCompleted = true

                    if (orientation == LEFT) {
                        if (event.x - touchStartX > FLIP_DISTANCE) {
                            // 向屏幕内侧滑动 - 停顿250ms 打开最近任务，不停顿则“返回”
                            if (isLongTimeGesture) performLongClick() else performClick()
                        }
                    } else if (orientation == RIGHT) {
                        if (touchStartX - event.x > FLIP_DISTANCE) {
                            // 向屏幕内侧滑动 - 停顿250ms 打开最近任务，不停顿则“返回”
                            if (isLongTimeGesture) performLongClick() else performClick()
                        }
                    } else if (orientation == BOTTOM) {
                        if (touchStartY - event.y > FLIP_DISTANCE) {
                            if (isLongTimeGesture) performLongClick() else performClick()
                        }
                    }
                    cleartEffect()
                }
                event.action == MotionEvent.ACTION_CANCEL -> {
                    cleartEffect()
                    return true
                }
                event.action == MotionEvent.ACTION_OUTSIDE -> {
                    cleartEffect()
                    return false
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
        // resumeBackupSize()
        invalidate()
        isTouchDown = false

        if (va != null && va!!.isRunning) {
            va!!.cancel()
        }
        va = ValueAnimator.ofFloat(this.currentGraphSize, 5f)
        va!!.run {
            duration = 200
            interpolator = AccelerateInterpolator()
            addUpdateListener { animation ->
                currentGraphSize = animation.animatedValue as Float
                if (touchCurrentX > 0 || touchCurrentY > 0) {
                    if (currentGraphSize < iconRadius) {
                        touchCurrentX = 0f
                        touchCurrentY = 0f
                        gestureStartTime = 0
                    }
                }
                if (currentGraphSize <= 6f && !isTouchDown) {
                    resumeBackupSize()
                }
                invalidate()
            }
            start()
        }
    }

    /**
     * 计算手势中的图标显示位置
     */
    private fun getEffectIconRectF(centerX: Float, centerY: Float): RectF {
        return RectF(centerX - iconRadius, centerY - iconRadius, centerX + iconRadius, centerY + iconRadius)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (currentGraphSize < 6f) {
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

                path.run {
                    moveTo((centerX + graphHeight), (centerY - cushion))
                    quadTo((centerX + graphHeight * 2), (centerY - graphWidth), centerX, (centerY - graphWidth / 2)) // 左侧平缓弧线
                    quadTo((centerX - graphHeight * 2.4f), centerY, centerX, (centerY + graphWidth / 2)) // 顶部圆拱
                    quadTo((centerX + graphHeight * 2), (centerY + graphWidth), (centerX + graphHeight), (centerY + cushion)) // 右侧平缓弧线
                }

                canvas.drawPath(path, p)

                if (drawIcon && touchCurrentX - touchStartX > FLIP_DISTANCE) {
                    try {
                        canvas.drawBitmap(
                                if (isLongTimeGesture) touch_tasks else touch_arrow_right,
                                null,
                                getEffectIconRectF(centerX, centerY),
                                p)
                    } catch (ex: Exception) {
                        Log.e("图标渲染错误", "" + ex.message)
                        drawIcon = false
                    }
                }
            }
        } else if (orientation == RIGHT) {
            if (touchStartY > 0) {
                val path = Path()
                val graphHeight = currentGraphSize
                val graphWidth = effectWidth
                val centerX = width - graphHeight
                val centerY = touchStartY // height / 2

                path.run {
                    moveTo((centerX + graphHeight), (centerY - cushion))
                    quadTo((centerX + graphHeight * 2), (centerY - graphWidth), centerX, (centerY - graphWidth / 2)) // 左侧平缓弧线
                    quadTo((centerX - graphHeight * 2.4f), centerY, centerX, (centerY + graphWidth / 2)) // 顶部圆拱
                    quadTo((centerX + graphHeight * 2), (centerY + graphWidth), (centerX + graphHeight), (centerY + cushion)) // 右侧平缓弧线
                }

                canvas.drawPath(path, p)

                if (drawIcon && touchStartX - touchCurrentX > FLIP_DISTANCE) {
                    try {
                        canvas.drawBitmap(
                                if (isLongTimeGesture) touch_tasks else touch_arrow_left,
                                null,
                                getEffectIconRectF(centerX, centerY),
                                p)
                    } catch (ex: Exception) {
                        Log.e("图标渲染错误", "" + ex.message)
                        drawIcon = false
                    }
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

                path.run {
                    moveTo((centerX - cushion), (centerY + graphHeight)) //贝赛尔的起始点moveTo(x,y)
                    quadTo((centerX - graphWidth), (centerY + graphHeight * 2), (centerX - graphWidth / 2), centerY) // 左侧平缓弧线
                    quadTo(centerX, (centerY - graphHeight * 2.5f), (centerX + graphWidth / 2), centerY) // 顶部圆拱
                    quadTo((centerX + graphWidth), (centerY + graphHeight * 2), (centerX + cushion), (centerY + graphHeight)) // 右侧平缓弧线
                }

                canvas.drawPath(path, p)

                if (drawIcon && touchStartY - touchCurrentY > FLIP_DISTANCE) {
                    try {
                        canvas.drawBitmap(
                                if (isLongTimeGesture) touch_tasks else touch_home,
                                null,
                                getEffectIconRectF(centerX, centerY),
                                p)
                    } catch (ex: Exception) {
                        Log.e("图标渲染错误", "" + ex.message)
                        drawIcon = false
                    }
                }
            }
        }
    }

    private var drawIcon = true

    private val touch_arrow_left = BitmapFactory.decodeResource(context.getResources(), R.drawable.touch_arrow_left)
    private val touch_arrow_right = BitmapFactory.decodeResource(context.getResources(), R.drawable.touch_arrow_right)
    private val touch_tasks = BitmapFactory.decodeResource(context.getResources(), R.drawable.touch_tasks)
    private val touch_home = BitmapFactory.decodeResource(context.getResources(), R.drawable.touch_home)
}
