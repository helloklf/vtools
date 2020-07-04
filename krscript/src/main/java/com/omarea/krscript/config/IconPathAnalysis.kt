package com.omarea.krscript.config

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.omarea.krscript.R
import com.omarea.krscript.model.ClickableNode


class IconPathAnalysis {
    // 获取快捷方式的图标
    fun loadLogo(context: Context, clickableNode: ClickableNode): Drawable {
        return loadLogo(context, clickableNode, true)!!
    }

    // 获取快捷方式的图标
    fun loadLogo(context: Context, clickableNode: ClickableNode, useDefault: Boolean): Drawable? {
        if (!clickableNode.logoPath.isEmpty()) {
            val inputStream = PathAnalysis(context, clickableNode.pageConfigDir).parsePath(clickableNode.logoPath)
            inputStream?.run {
                return bitmap2Drawable(BitmapFactory.decodeStream(this)) // BitmapDrawable.createFromStream(inputStream, "")
            }
        }
        if (!clickableNode.iconPath.isEmpty()) {
            val inputStream = PathAnalysis(context, clickableNode.pageConfigDir).parsePath(clickableNode.iconPath)
            inputStream?.run {
                return bitmap2Drawable(BitmapFactory.decodeStream(this)) // BitmapDrawable.createFromStream(inputStream, "")
            }
        }
        return if (useDefault) context.getDrawable(R.drawable.kr_shortcut_logo)!! else null
    }

    fun loadIcon(context: Context, clickableNode: ClickableNode): Drawable? {
        if (!clickableNode.iconPath.isEmpty()) {
            val inputStream = PathAnalysis(context, clickableNode.pageConfigDir).parsePath(clickableNode.iconPath)
            inputStream?.run {
                return bitmap2Drawable(BitmapFactory.decodeStream(this)) // BitmapDrawable.createFromStream(inputStream, "")
            }
        }
        return null
    }

    // Bitmap转换成Drawable
    fun bitmap2Drawable(bitmap: Bitmap): Drawable {
        return BitmapDrawable(bitmap)
    }
}
