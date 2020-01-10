package com.omarea.vtools.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.omarea.store.BatteryHistoryStore
import com.omarea.ui.AdapterBatteryStats
import com.omarea.vtools.R
import com.omarea.vtools.activities.AccessibilityKeySettings
import kotlinx.android.synthetic.main.fragment_battery_stats.*
import kotlinx.android.synthetic.main.list_item_text.view.*


class FragmentNav : Fragment(), View.OnClickListener {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_nav, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nav = view.findViewById<LinearLayout>(R.id.nav)
        for (index in 1..nav.childCount step 2){
            val grid: GridLayout = nav.getChildAt(index) as GridLayout
            for (index2 in 0 until grid.childCount){
                bindClickEvent(grid.getChildAt(index2))
            }
        }
    }

    private fun bindClickEvent(view: View) {
        view.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (isDetached) {
            return
        }
    }

    override fun onClick(v: View?) {
        v?.run {
            val transaction = activity!!.supportFragmentManager.beginTransaction()
            transaction.setCustomAnimations(R.animator.fragment_enter, R.animator.fragment_exit)
            var fragment: Fragment? = null

            when (id) {
                R.id.nav_freeze -> fragment = FragmentFreeze.createPage()
                R.id.nav_applictions -> fragment = FragmentApplistions.createPage()
                R.id.nav_swap -> fragment = FragmentSwap.createPage()
                R.id.nav_battery -> fragment = FragmentBattery.createPage()
                R.id.nav_img -> fragment = FragmentImg.createPage()
                R.id.nav_battery_stats -> fragment = FragmentBatteryStats.createPage()
                R.id.nav_core_control -> fragment = FragmentCpuControl.newInstance()
                R.id.nav_paypal -> fragment = FragmentPay.createPage()
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
                R.id.nav_system_scene -> {
                    fragment = FragmentSystemScene.createPage()
                }
                R.id.nav_app_magisk -> {
                    fragment = FragmentMagisk.createPage()
                }
                // TODO:R.id.nav_additional -> fragment = FragmentAddin.createPage(themeMode)
                R.id.nav_keyevent -> {
                    try {
                        val intent = Intent(activity, AccessibilityKeySettings::class.java)
                        startActivity(intent)
                    } catch (ex: Exception) {
                    }
                }
            }

            if (fragment != null) {
                // configlist_tabhost.currentTab = 1

                transaction.disallowAddToBackStack()
                transaction.replace(R.id.main_content, fragment)
                //transaction.addToBackStack(item.title.toString());
                transaction.commitAllowingStateLoss()
                activity!!.title = (v as TextView).text.toString()
                //item.isChecked = true
            }
        }
    }
}
