package com.omarea.vtools.activities

import android.Manifest
import android.app.Activity
import android.app.UiModeManager
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
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.PermissionChecker
import com.omarea.common.ui.ThemeMode
import com.omarea.store.SpfConfig
import com.omarea.vtools.R

object ThemeSwitch {
    private var globalSPF: SharedPreferences? = null

    private fun checkPermission(context: Context, permission: String): Boolean = PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED

    internal fun switchTheme(activity: Activity): ThemeMode {
        val themeMode = ThemeMode()
        if (globalSPF == null) {
            globalSPF = activity.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        }

        val theme = globalSPF!!.getInt(SpfConfig.GLOBAL_SPF_THEME, -1)

        // 设置壁纸作为背景需要读取外置存储权限（如果没权限，就恢复默认主题）
        if (theme == 10 && !(checkPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) && checkPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
            globalSPF!!.edit().remove(SpfConfig.GLOBAL_SPF_THEME).apply()
            return switchTheme(activity)
        }

        val uiModeManager = activity.applicationContext.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        themeMode.isDarkMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        val themeId = (
                // AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                if (themeMode.isDarkMode) {
                    themeMode.isLightStatusBar = false
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    R.style.AppThemeNoActionBarNight
                } else {
                    themeMode.isLightStatusBar = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    R.style.AppThemeWhite
                })
        if (activity is AppCompatActivity) {
            activity.getDelegate().setLocalNightMode(AppCompatDelegate.getDefaultNightMode())
        }
        activity.setTheme(themeId)

        if (themeMode.isLightStatusBar) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }

        return themeMode
    }

    private fun isDarkColor(wallPaper: Drawable): Boolean {
        // 根据壁纸色彩设置主题
        val bitmap = (wallPaper as BitmapDrawable).bitmap
        val h = bitmap.height - 1
        val w = bitmap.width - 1

        var darkPoint = 0
        var lightPoint = 0

        // 采样点数
        val pointCount = if (h > 24 && w > 24) 24 else 1

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
}
