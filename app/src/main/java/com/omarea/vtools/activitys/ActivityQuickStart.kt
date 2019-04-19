package com.omarea.vtools.activitys

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.omarea.shared.SpfConfig
import com.omarea.shell.CheckRootStatus
import com.omarea.shell.KeepShellPublic
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_start_splash.*
import java.lang.ref.WeakReference

class ActivityQuickStart : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeSwitch.switchTheme(this)
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
            Toast.makeText(this, "该快捷方式无效！", Toast.LENGTH_SHORT).show()
            start_state_text.text = "无效的快捷方式！"
        } else {
            checkRoot(CheckRootSuccess(this))
        }
    }

    private class CheckRootSuccess(context: ActivityQuickStart) : Runnable {
        private var context: WeakReference<ActivityQuickStart>;
        override fun run() {
            context.get()!!.start_state_text.text = "检查并获取必需权限..."
            context.get()!!.hasRoot = true

            // TODO:启动应用
            context.get()!!.startApp();
        }

        init {
            this.context = WeakReference(context)
        }
    }

    private fun startApp() {
        try {
            val app = intent.getStringExtra("packageName");
            val pm = packageManager

            val appInfo = pm.getApplicationInfo(app, 0)
            if (appInfo != null) {
                if (!appInfo.enabled) {
                    KeepShellPublic.doCmdSync("pm enable ${app};")
                }

                val appIntent = pm.getLaunchIntentForPackage(app)
                if (appIntent != null) {
                    appIntent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                    // LauncherApps().startMainActivity()
                    appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(appIntent)
                    finish()
                } else {
                    start_state_text.text = "该应用无法启动！"
                }
            } else {
                start_state_text.text = "此应用已被卸载！"
            }
        } catch (ex: Exception) {
            Toast.makeText(this, "启动应用失败，" + ex.message, Toast.LENGTH_LONG).show()
            start_state_text.text = "启动应用失败！"
        }
        finish()
        return
    }

    private var hasRoot = false
    @SuppressLint("ApplySharedPref", "CommitPrefEdits")
    private fun checkRoot(next: Runnable) {
        val globalConfig = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        val disableSeLinux = globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)
        CheckRootStatus(this, next, disableSeLinux).forceGetRoot()
    }
}