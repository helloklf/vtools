package com.omarea.vboot

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.omarea.shared.Consts
import com.omarea.shared.SpfConfig
import com.omarea.shared.helper.NotifyHelper
import com.omarea.shell.SuDo


class ActivityQuickSwitchMode : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //val window = window
        //val wl = window.attributes
        //wl.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        //wl.alpha = 0f //这句就是设置窗口里崆件的透明度的．０.０全透明．１.０不透明．
        //window.attributes = wl
        if(this.intent != null && this.intent.extras != null) {
            val parameterValue = this.intent.getStringExtra("packageName");
            if(parameterValue == null || parameterValue.isEmpty()) {
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
                    Toast.makeText(this,"为微工具箱授权显示悬浮窗权限，从而在应用中快速切换模式！",Toast.LENGTH_SHORT).show();
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

    private fun getModName(mode:String) : String {
        when(mode) {
            "powersave" ->      return "省电模式"
            "performance" ->      return "性能模式"
            "fast" ->      return "极速模式"
            "balance" ->   return "均衡模式"
            else ->         return "未知模式"
        }
    }

    private fun getAppName(packageName: String): String{
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0);
            return  packageInfo.applicationInfo.loadLabel(packageManager).toString()
        } catch (ex : Exception) {
            return  packageName;
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun quickSwitchMode(){
        val parameterValue = this.intent.getStringExtra("packageName");
        if(packageName == null || parameterValue.isEmpty()) {
            return
        }
        val spfPowercfg = getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
        val mode = spfPowercfg.getString(parameterValue, "balance");
        var index = 0;

        when(mode) {
            "powersave" ->      index = 0
            "balance" ->   index = 1
            "performance" ->      index = 2
            "fast" ->      index = 3
            else ->         index = 1
        }

        AlertDialog.Builder(this)
                .setPositiveButton(R.string.btn_confirm, { dialog, which ->
                    try {
                        var selectedMode = ""
                        when(index) {
                            0 -> selectedMode = "powersave"
                            1 -> selectedMode = "balance"
                            2 -> selectedMode = "performance"
                            3 -> selectedMode = "fast"
                            4 -> selectedMode = "igoned"
                        }
                        spfPowercfg.edit().putString(parameterValue, selectedMode).commit()
                        SuDo(this).execCmd(String.format(Consts.ToggleMode, selectedMode));
                        NotifyHelper(this).notify("${getModName(selectedMode)} -> $parameterValue" , parameterValue)

                        val intent = getPackageManager().getLaunchIntentForPackage(parameterValue);
                        startActivity(intent);
                    } catch (ex : Exception) {
                        Toast.makeText(this, ex.message, Toast.LENGTH_SHORT).show()
                    } finally {
                        finish()
                    }
                })
                .setNegativeButton(R.string.btn_cancel, { dialog, which ->
                    finish()
                })
                .setCancelable(true)
                .setOnCancelListener {
                    finish()
                }
                .setTitle(getAppName(parameterValue))
                .setSingleChoiceItems(arrayOf("省电模式", "均衡模式", "游戏模式", "极速模式", "加入“忽略列表”"),index, { dialog, which ->
                    index = which;
                })
                .create()
                .show()
    }

}
