package com.omarea.vtools.fragments

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.omarea.Scene
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ThemeMode
import com.omarea.kr.KrScriptConfig
import com.omarea.library.shell.BatteryUtils
import com.omarea.permissions.CheckRootStatus
import com.omarea.shell_utils.BackupRestoreUtils
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.vtools.R
import com.omarea.vtools.activities.*
import com.omarea.vtools.dialogs.DialogXposedGlobalConfig
import com.omarea.xposed.XposedCheck
import com.projectkr.shell.OpenPageHelper
import kotlinx.android.synthetic.main.fragment_nav.*

class FragmentNav : Fragment(), View.OnClickListener {
    private lateinit var themeMode: ThemeMode

    companion object {
        fun createPage(themeMode: ThemeMode): Fragment {
            val fragment = FragmentNav()
            fragment.themeMode = themeMode;
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_nav, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nav = view.findViewById<LinearLayout>(R.id.nav)
        for (index in 1..nav.childCount) {
            val ele = nav.getChildAt(index)
            if (ele is GridLayout) {
                for (index2 in 0 until ele.childCount) {
                    bindClickEvent(ele.getChildAt(index2))
                }
            }
        }

        // 激活辅助服务按钮
        nav_scene_service_not_active.setOnClickListener {
            startService()
        }
    }

    private fun startService() {
        AccessibleServiceHelper().stopSceneModeService(activity!!.applicationContext)

        /* 使用ROOT权限激活辅助服务会导致某些授权拿不到，导致事件触发不完整 */
        /*

        val dialog = ProgressBarDialog(context!!)
        dialog.showDialog("尝试使用ROOT权限开启服务...")
        Thread(Runnable {
            if (!AccessibleServiceHelper().startSceneModeService(context!!)) {
                try {
                    myHandler.post {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        // intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    myHandler.post {
                        dialog.hideDialog()
                    }
                }
            } else {
                myHandler.post {
                    dialog.hideDialog()
                    btn_config_service_not_active.visibility = if (AccessibleServiceHelper().serviceRunning(context!!)) View.GONE else View.VISIBLE
                }
            }
        }).start()
        */
        Scene.toast("请在系统设置里激活[Scene - 场景模式]选项", Toast.LENGTH_SHORT)
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)
            startActivity(intent)
        } catch (e: Exception) {
        }
    }

    private fun bindClickEvent(view: View) {
        view.setOnClickListener(this)
        if (!CheckRootStatus.lastCheckResult && "root".equals(view.getTag())) {
            view.isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (isDetached) {
            return
        }

        // 辅助服务激活状态
        val serviceState = AccessibleServiceHelper().serviceRunning(context!!)
        nav_scene_service_not_active.visibility = if (serviceState) View.GONE else View.VISIBLE

        activity!!.title = getString(R.string.app_name)
    }

    private fun tryOpenApp(packageName: String) {
        val pm = context!!.packageManager
        if (packageName.equals("com.omarea.gesture")) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.setComponent(ComponentName("com.omarea.gesture", "com.omarea.gesture.SettingsActivity"))
                startActivity(intent)
                return
            } catch (ex: java.lang.Exception) {
            }
        } else if (packageName.equals("com.omarea.filter")) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.setComponent(ComponentName("com.omarea.filter", "com.omarea.filter.SettingsActivity"))
                startActivity(intent)
                return
            } catch (ex: java.lang.Exception) {
            }
        }

        try {
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                return
            }
        } catch (ex: java.lang.Exception) {
        }

        openUrl("https://www.coolapk.com/apk/" + packageName)
        /*
            Uri uri = Uri.parse("market://details?id=" + appPkg);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (marketPkg != null) {// 如果没给市场的包名，则系统会弹出市场的列表让你进行选择。
                intent.setPackage(marketPkg);
            }
            try {
                context.startActivity(intent);
                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        */
    }

    override fun onClick(v: View?) {
        v?.run {
            if (!CheckRootStatus.lastCheckResult && "root".equals(getTag())) {
                Toast.makeText(context, "没有获得ROOT权限，不能使用本功能", Toast.LENGTH_SHORT).show()
                return
            }

            when (id) {
                R.id.nav_freeze -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setClassName("com.omarea.vtools", "com.omarea.vtools.activities.ActivityFreezeApps2")
                    startActivity(intent)
                    return
                }
                R.id.nav_applictions -> {
                    val intent = Intent(context, ActivityApplistions::class.java)
                    startActivity(intent)
                    return
                }
                R.id.nav_swap -> {
                    val intent = Intent(context, ActivitySwap::class.java)
                    startActivity(intent)
                    return
                }
                R.id.nav_battery -> {
                    val batteryUtils = BatteryUtils()
                    if (batteryUtils.qcSettingSupport() || batteryUtils.bpSettingSupport()) {
                        val intent = Intent(context, ActivityBattery::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(context, "此功能不支持你的手机", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                R.id.nav_charge -> {
                    val intent = Intent(context, ActivityCharge::class.java)
                    startActivity(intent)
                    return
                }
                R.id.nav_img -> {
                    if (BackupRestoreUtils.isSupport()) {
                        val intent = Intent(context, ActivityImg::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(context, "此功能不支持你的手机", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                R.id.nav_battery_stats -> {
                    val intent = Intent(context, ActivityBatteryStats::class.java)
                    startActivity(intent)
                    return
                }
                R.id.nav_core_control -> {
                    val intent = Intent(context, ActivityCpuControl::class.java)
                    startActivity(intent)
                    return
                }
                R.id.nav_miui_thermal -> {
                    val intent = Intent(context, ActivityMiuiThermal::class.java)
                    startActivity(intent)
                    return
                }
                R.id.nav_qq -> {
                    val key = "6ffXO4eTZVN0eeKmp-2XClxizwIc7UIu" //""e-XL2In7CgIpeK_sG75s-vAiu7n5DnlS"
                    val intent = Intent()
                    intent.data = Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D$key")
                    // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    return try {
                        startActivity(intent)
                    } catch (e: Exception) {
                    }
                }
                R.id.nav_share -> {
                    val sendIntent = Intent()
                    sendIntent.action = Intent.ACTION_SEND
                    sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_link))
                    sendIntent.type = "text/plain"
                    startActivity(sendIntent)
                }
                R.id.nav_app_scene -> {
                    val intent = Intent(context, ActivityAppConfig2::class.java)
                    startActivity(intent)
                    return
                }
                R.id.nav_cpu_modes -> {
                    val intent = Intent(context, ActivityCpuModes::class.java)
                    startActivity(intent)
                    return
                }
                R.id.nav_system_scene -> {
                    val intent = Intent(context, ActivitySystemScene::class.java)
                    startActivity(intent)
                    return
                }
                R.id.nav_auto_click -> {
                    val intent = Intent(context, ActivityAutoClick::class.java)
                    startActivity(intent)
                    return
                }
                R.id.nav_app_magisk -> {
                    val intent = Intent(context, ActivityMagisk::class.java)
                    startActivity(intent)
                    return
                }
                R.id.nav_xposed_app -> {
                    xposedCheck {
                        val intent = Intent(context, ActivityAppXposedConfig::class.java)
                        startActivity(intent)
                    }
                    return
                }
                R.id.nav_xposed_global -> {
                    xposedCheck {
                        DialogXposedGlobalConfig(activity!!).show()
                    }
                    return
                }
                R.id.nav_gesture -> {
                    tryOpenApp("com.omarea.gesture")
                    return
                }
                R.id.nav_filter -> {
                    tryOpenApp("com.omarea.filter")
                    return
                }
                R.id.nav_processes -> {
                    val intent = Intent(context, ActivityProcess::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    return
                }
                R.id.nav_fps_chart -> {
                    val serviceState = AccessibleServiceHelper().serviceRunning(context!!)
                    if (serviceState) {
                        val intent = Intent(context, ActivityFpsChart::class.java)
                        startActivity(intent)
                    } else {
                        Scene.toast("请在系统设置里激活[Scene - 场景模式]辅助服务", Toast.LENGTH_SHORT)
                    }
                    return
                }
                R.id.nav_additional -> {
                    val intent = Intent(context, ActivityAddin::class.java)
                    startActivity(intent)
                    return
                }
                R.id.nav_additional_all -> {
                    val krScriptConfig = KrScriptConfig().init(context!!)
                    val activity = activity!!
                    krScriptConfig.pageListConfig?.run {
                        OpenPageHelper(activity).openPage(this.apply {
                            title = getString(R.string.menu_additional)
                        })
                    }
                    return
                }
            }
        }
    }

    private fun installVAddin() {
        DialogHelper.warning(context!!, getString(R.string.scene_addin_miss), getString(R.string.scene_addin_miss_desc), {
            try {
                val uri = Uri.parse("http://vtools.omarea.com/")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(context, "启动在线页面失败！", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun xposedCheck(onPass: Runnable) {
        var vAddinsInstalled: Boolean
        try {
            vAddinsInstalled = context!!.packageManager.getPackageInfo("com.omarea.vaddin", 0) != null
        } catch (ex: Exception) {
            vAddinsInstalled = false
        }
        if (vAddinsInstalled) {
            if (XposedCheck.xposedIsRunning()) {
                onPass.run()
            } else {
                Toast.makeText(context, "请先在Xposed管理器中重新勾选“Scene”，并重启手机", Toast.LENGTH_LONG).show()
            }
        } else {
            installVAddin()
        }
    }

    private fun openUrl(link: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (ex: Exception) {
        }
    }
}
