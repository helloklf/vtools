package com.omarea.common.ui

import android.app.Activity
import android.app.Dialog
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView

class BlurBackground(private val activity: Activity) {
    private var dialogBg: ImageView? = null
    private var originalW = 0
    private var originalH = 0
    private var mHandler: Handler = Handler(Looper.getMainLooper())

    private fun captureScreen(activity: Activity): Bitmap? {
        activity.window.decorView.destroyDrawingCache() //先清理屏幕绘制缓存(重要)
        activity.window.decorView.isDrawingCacheEnabled = true
        var bmp: Bitmap = activity.window.decorView.drawingCache
        //获取原图尺寸
        originalW = bmp.getWidth()
        originalH = bmp.getHeight()
        //对原图进行缩小，提高下一步高斯模糊的效率
        bmp = Bitmap.createScaledBitmap(bmp, originalW / 4, originalH / 4, false)
        return bmp
    }

    private fun asyncRefresh(`in`: Boolean) {
        //淡出淡入效果的实现
        if (`in`) {    //淡入效果
            Thread {
                var i = 0
                while (i < 256) {
                    refreshUI(i) //在UI线程刷新视图
                    try {
                        Thread.sleep(4)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    i += 5
                }
            }.start()
        } else {    //淡出效果
            Thread {
                var i = 255
                while (i >= 0) {
                    refreshUI(i) //在UI线程刷新视图
                    try {
                        Thread.sleep(4)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    i -= 5
                }
                //当淡出效果完毕后发送消息给mHandler把对话框背景设为不可见
                mHandler.sendEmptyMessage(0)
            }.start()
        }
    }

    private fun runOnUiThread(runnable: Runnable) {
        mHandler.post(runnable)
    }

    private fun refreshUI(i: Int) {
        runOnUiThread(Runnable { dialogBg?.setImageAlpha(i) })
    }

    private fun hideBlur() {
        //把对话框背景隐藏
        asyncRefresh(false)
        System.gc()
    }

    private fun blur(bitmap: Bitmap): Bitmap? {
        //使用RenderScript对图片进行高斯模糊处理
        val output = Bitmap.createBitmap(bitmap) // 创建输出图片
        val rs: RenderScript = RenderScript.create(activity) // 构建一个RenderScript对象
        val gaussianBlue: ScriptIntrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs)) //
        // 创建高斯模糊脚本
        val allIn: Allocation = Allocation.createFromBitmap(rs, bitmap) // 开辟输入内存
        val allOut: Allocation = Allocation.createFromBitmap(rs, output) // 开辟输出内存
        val radius = 10f //设置模糊半径
        gaussianBlue.setRadius(radius) // 设置模糊半径，范围0f<radius<=25f
        gaussianBlue.setInput(allIn) // 设置输入内存
        gaussianBlue.forEach(allOut) // 模糊编码，并将内存填入输出内存
        allOut.copyTo(output) // 将输出内存编码为Bitmap，图片大小必须注意
        rs.destroy()
        //rs.releaseAllContexts(); // 关闭RenderScript对象，API>=23则使用rs.releaseAllContexts()
        return output
    }

    private fun handleBlur() {
        dialogBg?.run {
            var bp = captureScreen(activity)
            if (bp == null) {
                return
            }

            bp = blur(bp) //对屏幕截图模糊处理
            //将模糊处理后的图恢复到原图尺寸并显示出来
            bp = Bitmap.createScaledBitmap(bp, originalW, originalH, false)
            setImageBitmap(bp)
            setVisibility(View.VISIBLE)
            //防止UI线程阻塞，在子线程中让背景实现淡入效果
            asyncRefresh(true)
        }
    }

    fun setScreenBgLight(dialog: Dialog) {
        val window: Window? = dialog.getWindow()
        val lp: WindowManager.LayoutParams
        if (window != null) {
            lp = window.getAttributes()
            lp.dimAmount = 0.2f
            window.setAttributes(lp)
        }
        handleBlur()
    }

}