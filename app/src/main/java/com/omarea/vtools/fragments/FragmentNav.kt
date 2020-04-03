package com.omarea.vtools.fragments

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.omarea.common.ui.ThemeMode
import com.omarea.kr.KrScriptConfig
import com.omarea.permissions.CheckRootStatus
import com.omarea.shell_utils.BackupRestoreUtils
import com.omarea.vtools.R
import com.projectkr.shell.OpenPageHelper

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
        for (index in 1..nav.childCount step 2) {
            val grid: GridLayout = nav.getChildAt(index) as GridLayout
            for (index2 in 0 until grid.childCount) {
                bindClickEvent(grid.getChildAt(index2))
            }
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
            val transaction = activity!!.supportFragmentManager.beginTransaction()
            transaction.setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            // transaction.setCustomAnimations(R.animator.fragment_enter, R.animator.fragment_exit)
            var fragment: Fragment? = null

            when (id) {
                R.id.nav_freeze -> fragment = FragmentFreeze.createPage()
                R.id.nav_applictions -> fragment = FragmentApplistions.createPage()
                R.id.nav_swap -> fragment = FragmentSwap.createPage()
                R.id.nav_battery -> fragment = FragmentBattery.createPage()
                R.id.nav_charge -> fragment = FragmentCharge()
                R.id.nav_img -> {
                    if (!BackupRestoreUtils.isSupport()) {
                        Toast.makeText(context, "此功能不支持你的手机", Toast.LENGTH_SHORT).show()
                        return
                    }
                    fragment = FragmentImg.createPage()
                }
                R.id.nav_battery_stats -> fragment = FragmentBatteryStats.createPage()
                R.id.nav_core_control -> fragment = FragmentCpuControl.newInstance()
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
                    fragment = FragmentConfig.createPage()
                }
                R.id.nav_cpu_modes -> {
                    fragment = FragmentCpuModes.createPage()
                }
                R.id.nav_system_scene -> {
                    fragment = FragmentSystemScene.createPage()
                }
                R.id.nav_app_magisk -> {
                    fragment = FragmentMagisk.createPage()
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
                    fragment = FragmentProcess()
                }
                R.id.nav_additional -> fragment = FragmentAddin()
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

            if (fragment != null) {
                // configlist_tabhost.currentTab = 1

                // transaction.disallowAddToBackStack()
                transaction.replace(R.id.app_more, fragment)
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss()

                //item.isChecked = true
            }
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
