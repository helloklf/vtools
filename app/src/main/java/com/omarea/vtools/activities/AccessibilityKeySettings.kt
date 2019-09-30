package com.omarea.vtools.activities

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.*
import com.omarea.store.SpfConfig
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_keyevent_settings.*
import java.util.*


class AccessibilityKeySettings : AppCompatActivity() {
    private lateinit var spf: SharedPreferences
    private lateinit var spfOther: SharedPreferences

    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()
    }

    override fun onResume() {
        super.onResume()

        val serviceState = AccessibleServiceHelper().serviceRunning(this, "AccessibilityKey")
        key_event_state.text = if (serviceState) getString(R.string.accessibility_running) else getString(R.string.accessibility_stoped)
    }

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        spf = getSharedPreferences(SpfConfig.KEY_EVENT_SPF, Context.MODE_PRIVATE)
        spfOther = getSharedPreferences(SpfConfig.KEY_EVENT_ONTHER_CONFIG_SPF, Context.MODE_PRIVATE)

        ThemeSwitch.switchTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyevent_settings)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        // setTitle(R.string.app_name)

        // 显示返回按钮
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        setSwitchClick()
        key_event_state.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(applicationContext, "请在无障碍设置中找到“Scene-按键辅助”并激活，从而使用按键辅助功能！", Toast.LENGTH_LONG).show()
            } catch (ex: Exception) {

            }
        }
    }

    @SuppressLint("ApplySharedPref")
    fun setSwitchClick() {
        /*
        val accessibilityEnabled = Settings.Secure.getInt(getApplicationContext().getContentResolver(), android.provider.Settings.Secure.ACCESSIBILITY_ENABLED)
        if (accessibilityEnabled === 1) {
            val settingValue = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }
        */
        val tags = arrayListOf("3", "4", "82", "187")
        val listItem = getEventOverride()
        val adapter = SimpleAdapter(this, listItem,
                R.layout.list_item_text,
                arrayOf("text"),
                intArrayOf(R.id.text))
        for (tag in tags) {
            val switch = key_sets.findViewWithTag<Switch>(tag)
            val propClick = switch.tag.toString() + "_click"
            val propLongClick = switch.tag.toString() + "_long_click"
            switch.isChecked = spf.contains(propClick) || spf.contains(propLongClick)
            val click = key_sets.findViewWithTag<Spinner>(propClick)
            val longClick = key_sets.findViewWithTag<Spinner>(propLongClick)
            click.adapter = adapter
            longClick.adapter = adapter
            click.isEnabled = switch.isChecked
            longClick.isEnabled = switch.isChecked
            switch.setOnClickListener {
                if (switch.isChecked) {
                    val clickValue = (click.selectedItem as HashMap<*, *>).get("key") as Int
                    val longClickValue = (longClick.selectedItem as HashMap<*, *>).get("key") as Int
                    spf.edit()
                            .putInt(propClick, clickValue)
                            .putInt(propLongClick, longClickValue)
                            .commit()
                    click.isEnabled = true
                    longClick.isEnabled = true
                } else {
                    spf.edit()
                            .remove(propClick)
                            .remove(propLongClick)
                            .commit()
                    click.isEnabled = false
                    longClick.isEnabled = false
                }
            }
            click.setSelection(getIndex(listItem, spf.getInt(propClick, Int.MIN_VALUE)))
            longClick.setSelection(getIndex(listItem, spf.getInt(propLongClick, Int.MIN_VALUE)))
            click.onItemSelectedListener = OnItemSelected(click, spf)
            longClick.onItemSelectedListener = OnItemSelected(longClick, spf)
        }

        key_event_vitual_touch_bar.isChecked = spfOther.getBoolean(SpfConfig.CONFIG_SPF_TOUCH_BAR, false)
        key_event_vitual_touch_bar.setOnClickListener {
            val isChecked = (it as Switch).isChecked
            if (Build.VERSION.SDK_INT >= 23 && isChecked) {
                if (Settings.canDrawOverlays(this)) {
                    spfOther.edit().putBoolean(SpfConfig.CONFIG_SPF_TOUCH_BAR, isChecked).commit()
                } else {
                    //若没有权限，提示获取
                    //val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    //startActivity(intent);
                    val intent = Intent()
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                    intent.data = Uri.fromParts("package", this.packageName, null)
                    Toast.makeText(applicationContext, "为Scene授权显示悬浮窗权限，从而在使用虚拟导航条！", Toast.LENGTH_SHORT).show();
                    (it as Switch).isChecked = !isChecked
                }
            } else {
                spfOther.edit().putBoolean(SpfConfig.CONFIG_SPF_TOUCH_BAR, isChecked).commit()
            }
        }
    }

    private class OnItemSelected(private var spinner: Spinner, private var spf: SharedPreferences) : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            //
        }

        @SuppressLint("ApplySharedPref")
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if ((parent as Spinner).isEnabled)
                spf.edit().putInt(spinner.tag.toString(), (spinner.selectedItem as HashMap<*, *>).get("key") as Int).commit()
        }
    }

    private fun getEventOverride(): ArrayList<HashMap<String, Any>> {
        return ArrayList<HashMap<String, Any>>().apply {
            add(HashMap<String, Any>().apply {
                put("text", "默认")
                put("key", Int.MIN_VALUE)
            })
            add(HashMap<String, Any>().apply {
                put("text", "返回")
                put("key", AccessibilityService.GLOBAL_ACTION_BACK)
            })
            add(HashMap<String, Any>().apply {
                put("text", "主页")
                put("key", AccessibilityService.GLOBAL_ACTION_HOME)
            })
            /*
            add(HashMap<String, Any>().apply {
                put("text", "菜单")
                put("key", AccessibilityService.)
            })
            */
            add(HashMap<String, Any>().apply {
                put("text", "最近任务")
                put("key", AccessibilityService.GLOBAL_ACTION_RECENTS)
            })
            add(HashMap<String, Any>().apply {
                put("text", "电源界面")
                put("key", AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
            })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                add(HashMap<String, Any>().apply {
                    put("text", "分屏")
                    put("key", AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                })
            }
        }
    }

    private fun getIndex(items: ArrayList<HashMap<String, Any>>, value: Int): Int {
        for (index in 0 until items.size) {
            if (items[index].get("key") == value) return index
        }
        return 0
    }


    // 通知辅助服务配置变化
    private fun notifyService() {
        if (AccessibleServiceHelper().serviceRunning(this.applicationContext)) {
            val intent = Intent(getString(R.string.scene_keyeventchange_action))
            sendBroadcast(intent)
        }
    }

    public override fun onPause() {
        this.notifyService()
        super.onPause()
    }
}
