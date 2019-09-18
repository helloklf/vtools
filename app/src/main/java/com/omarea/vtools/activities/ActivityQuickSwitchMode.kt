package com.omarea.vtools.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.omarea.vtools.popup.FloatPowercfgSelector


class ActivityQuickSwitchMode : Activity() {
    override fun onPause() {
        super.onPause()
        this.finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //val window = window
        //val wl = window.attributes
        //wl.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        //wl.alpha = 0f //这句就是设置窗口里崆件的透明度的．０.０全透明．１.０不透明．
        //window.attributes = wl
        if (this.intent != null && this.intent.extras != null) {
            val parameterValue = this.intent.getStringExtra("packageName");
            if (parameterValue == null || parameterValue.isEmpty()) {
                return
            }
            if (Build.VERSION.SDK_INT >= 23) {
                if (Settings.canDrawOverlays(this)) {
                    FloatPowercfgSelector().showPopupWindow(this, parameterValue)
                } else {
                    //若没有权限，提示获取
                    //val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    //startActivity(intent);
                    val intent = Intent()
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                    intent.data = Uri.fromParts("package", this.packageName, null)
                    Toast.makeText(applicationContext, "为Scene授权显示悬浮窗权限，从而在应用中快速切换模式！", Toast.LENGTH_SHORT).show();
                }
            } else {
                FloatPowercfgSelector().showPopupWindow(this, parameterValue)
            }
            finishAndRemoveTask()
            //DisplayMetrics{density=2.75, width=1080, height=2160, scaledDensity=2.75, xdpi=403.411, ydpi=403.411}

            /*
            var metric = DisplayMetrics ()
            windowManager.defaultDisplay.getMetrics(metric)
            metric.density = metric.density / 1.6f;
            metric.scaledDensity = metric.scaledDensity / 1.6f;
            if (this.intent.extras != null && this.intent.extras.containsKey("parameterValue")) {
                quickSwitchMode()
            }
            */
        } else {
            finishAndRemoveTask()
        }
    }
}
