package com.omarea.scene_mode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import com.omarea.common.shared.BitmapUtil
import com.omarea.common.shared.FileWrite

public class LogoCacheManager(private var context: Context) {
    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            val backgroundDr = drawable.background
            val foregroundDr = drawable.foreground

            val drr = arrayOfNulls<Drawable>(2)
            drr[0] = backgroundDr
            drr[1] = foregroundDr

            val layerDrawable = LayerDrawable(drr)
            val width = layerDrawable.intrinsicWidth
            val height = layerDrawable.intrinsicHeight
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            layerDrawable.setBounds(0, 0, canvas.width, canvas.height)
            layerDrawable.draw(canvas)

            return bitmap
        }
        return null
    }

    private fun getCacheOutput(packageName: String): String {
        return FileWrite.getPrivateFilePath(context, "logo_cache/" + packageName + ".png")
    }

    public fun saveIcon(drawable: Drawable, packageName: String) {
        BitmapUtil().saveBitmapToSDCard(drawableToBitmap(drawable), getCacheOutput(packageName))
    }

    public fun loadIcon(packageName: String): Drawable? {
        val bitmap = BitmapUtil().getBitmapFromSDCard(getCacheOutput(packageName))
        if (bitmap == null) {
            return null
        }
        return BitmapDrawable(bitmap)
    }
}
