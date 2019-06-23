package com.omarea.vtools.activitys

import android.Manifest
import android.app.*
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.support.v4.content.PermissionChecker
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.omarea.common.ui.DialogHelper
import com.omarea.shared.SpfConfig
import com.omarea.vtools.R

object ThemeSwitch {
    private var globalSPF: SharedPreferences? = null

    private fun checkPermission(context: Context, permission: String): Boolean = PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED

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
                if (checkPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) && checkPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    setWallpaperTheme(activity)
                } else {
                    DialogHelper.helpInfo(activity, "", activity.getString(R.string.wallpaper_rw_permission))
                }
            }
        }
    }

    private fun setWallpaperTheme(activity: Activity) {
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

        val wallpape = WallpaperManager.getInstance(activity);
        val wallpaperInfo = wallpape.getWallpaperInfo()
        if (wallpaperInfo != null && wallpaperInfo.packageName != null) {
            activity.setTheme(R.style.AppThemeWallpaper)
            // activity.window.setBackgroundDrawable(activity.getDrawable(R.drawable.window_transparent));
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        } else {
            val wallpapeDrawable = wallpape.getDrawable();

            if (isDarkColor(wallpapeDrawable)) {
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

            activity.window.setBackgroundDrawable(wallpapeDrawable);
            // 使用壁纸高斯模糊作为窗口背景
            // activity.window.setBackgroundDrawable(BitmapDrawable(activity.resources, rsBlur((wallPaper as BitmapDrawable).bitmap, 25, activity)))
        }
    }

    private fun isDarkColor(wallPaper: Drawable): Boolean {
        // 根据壁纸色彩设置主题
        val bitmap = (wallPaper as BitmapDrawable).bitmap
        val h = bitmap.height - 1
        val w = bitmap.width - 1

        var darkPoint = 0
        var lightPoint = 0

        // 采样点数
        val pointCount = if(h > 8 && w > 8) 8 else 1

        for (i in 0..pointCount) {
            val y = h / pointCount * i
            val x = w / pointCount * i
            val pixel = bitmap.getPixel(x, y)

            // 获取颜色
            val redValue = Color.red(pixel)
            val blueValue = Color.blue(pixel)
            val greenValue = Color.green(pixel)

            if (redValue > 150 && blueValue > 150 && greenValue > 150) {
                lightPoint += 1
            } else {
                darkPoint += 1
            }
        }
        return darkPoint > lightPoint
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
