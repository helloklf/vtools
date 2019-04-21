package com.omarea.vtools.activitys

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import com.omarea.shared.SpfConfig
import com.omarea.shell.CheckRootStatus
import com.omarea.shell.KeepShellPublic
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_quick_start.*
import java.lang.ref.WeakReference

class ActivityQuickStart : Activity() {
    lateinit var appPackageName:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_quick_start)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        //  得到当前界面的装饰视图
        if (Build.VERSION.SDK_INT >= 21) {
            val decorView = getWindow().getDecorView();
            //让应用主题内容占用系统状态栏的空间,注意:下面两个参数必须一起使用 stable 牢固的
            val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            //设置状态栏颜色为透明
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        val extras = intent.extras
        if (extras == null || !extras.containsKey("packageName")) {
            start_state_text.text = "无效的快捷方式！"
        } else {
            appPackageName = intent.getStringExtra("packageName");
            val pm = packageManager

            val appInfo = pm.getApplicationInfo(appPackageName, 0)
            if (appInfo != null) {
                if (appInfo.enabled) {
                    startApp()
                } else {
                    checkRoot(CheckRootSuccess(this, appPackageName))
                }
            } else {
                start_state_text.text = "此应用已被卸载！"
            }
        }
    }

    private class CheckRootSuccess(context: ActivityQuickStart, private var appPackageName: String) : Runnable {
        private var context: WeakReference<ActivityQuickStart>;
        override fun run() {
            context.get()!!.start_state_text.text = "正在启动应用..."
            context.get()!!.hasRoot = true

            // TODO:启动应用
            KeepShellPublic.doCmdSync("pm enable ${appPackageName};")
            context.get()!!.startApp();
        }

        init {
            this.context = WeakReference(context)
        }
    }

    private fun startApp() {
        val pm = packageManager
        val appIntent = pm.getLaunchIntentForPackage(appPackageName)
        try {
            if (appIntent != null) {
                // LauncherApps().startMainActivity()
                appIntent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(appIntent)
                // overridePendingTransition(0, 0)
            } else {
                start_state_text.text = "该应用无法启动！"
            }
        } catch (ex: Exception) {
            start_state_text.text = "启动应用失败！"
        }
    }

    override fun onPause() {
        super.onPause()
        this.finish()
    }

    private var hasRoot = false
    @SuppressLint("ApplySharedPref", "CommitPrefEdits")
    private fun checkRoot(next: Runnable) {
        val globalConfig = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        val disableSeLinux = globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)
        CheckRootStatus(this, next, disableSeLinux).forceGetRoot()
    }
}