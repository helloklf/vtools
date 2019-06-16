package com.omarea.vtools.activitys

import android.app.Activity
import android.app.UiModeManager
import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import com.omarea.shared.SpfConfig
import com.omarea.vtools.R

object ThemeSwitch {
    private var globalSPF: SharedPreferences? = null

    internal fun switchTheme(activity: Activity) {
        if (globalSPF == null) {
            globalSPF = activity.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        }

        var theme = 1
        if (!globalSPF!!.contains(SpfConfig.GLOBAL_SPF_THEME) || globalSPF!!.getInt(SpfConfig.GLOBAL_SPF_THEME, -1) == -1) {
            val uiModeManager = activity.applicationContext.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            if (uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES) {
                theme = 8
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                theme = 9
            }
        } else {
            theme = globalSPF!!.getInt(SpfConfig.GLOBAL_SPF_THEME, 1)
        }

        when (theme) {
            0 -> {
                activity.setTheme(R.style.AppThemeBlue)
            }
            1 -> {
                activity.setTheme(R.style.AppThemeCyan)
            }
            2 -> {
                activity.setTheme(R.style.AppThemeGreen)
            }
            3 -> {
                activity.setTheme(R.style.AppThemeOrange)
            }
            4 -> {
                activity.setTheme(R.style.AppThemeRed)
            }
            5 -> {
                activity.setTheme(R.style.AppThemePink)
            }
            6 -> {
                activity.setTheme(R.style.AppThemePretty)
            }
            7 -> {
                activity.setTheme(R.style.AppThemeViolet)
            }
            8 -> {
                activity.setTheme(R.style.AppThemeNoActionBarNight)
            }
            9 -> {
                activity.setTheme(R.style.AppThemeWhite)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    } else {
                        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    }
                }
            }
            10 -> {
                /* // 根据夜间模式设置主题
                val uiModeManager = activity.applicationContext.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                if (uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES) {
                    activity.setTheme(R.style.AppThemeWallpaper)
                } else {
                    activity.setTheme(R.style.AppThemeWallpaperLight)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        } else {
                            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        }
                    }
                }
                */

                val wallPaper = WallpaperManager.getInstance(activity).getDrawable();

                // 根据壁纸色彩设置主题
                val bitmap = (wallPaper as BitmapDrawable).bitmap
                val x = bitmap.width / 2
                val y = bitmap.height / 2
                val pixel = bitmap.getPixel(x, y)
                // 获取颜色
                val redValue = Color.red(pixel)
                val blueValue = Color.blue(pixel)
                val greenValue = Color.green(pixel)

                if (redValue > 150 && blueValue > 150 && greenValue > 150) {
                    activity.setTheme(R.style.AppThemeWallpaperLight)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        } else {
                            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        }
                    }
                } else {
                    activity.setTheme(R.style.AppThemeWallpaper)
                }

                activity.window.setBackgroundDrawable(wallPaper);

                // 使用壁纸高斯模糊作为窗口背景
                // activity.window.setBackgroundDrawable(BitmapDrawable(activity.resources, rsBlur((wallPaper as BitmapDrawable).bitmap, 25, activity)))
            }
        }
    }

    private fun rsBlur(source: Bitmap, radius: Int, context: Context): Bitmap {
        val inputBmp = source
        val renderScript = RenderScript.create(context);

        // Allocate memory for Renderscript to work with
        //(2)
        val input = Allocation.createFromBitmap(renderScript, inputBmp);
        val output = Allocation.createTyped(renderScript, input.getType());
        //(3)
        // Load up an instance of the specific script that we want to use.
        val scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        //(4)
        scriptIntrinsicBlur.setInput(input);
        //(5)
        // Set the blur radius
        scriptIntrinsicBlur.setRadius(radius.toFloat());
        //(6)
        // Start the ScriptIntrinisicBlur
        scriptIntrinsicBlur.forEach(output);
        //(7)
        // Copy the output to the blurred bitmap
        output.copyTo(inputBmp);
        //(8)
        renderScript.destroy();

        return inputBmp;
    }

    internal enum class Themes {
        BLUE, // 蓝色
        CYAN, // 水鸭青
        GREEN, // 绿色
        ORANAGE, // 成色
        RED, // 红色
        PINK, // 粉色
        PRETTY, // 骚？
        VIOLET, // 紫色
        BLACK // 黑色
    }
}
