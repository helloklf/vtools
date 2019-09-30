package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Switch
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_adv_settings.*

class ActivityAdvSettings : AppCompatActivity() {
    private lateinit var spf: SharedPreferences
    private var myHandler = Handler()

    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()

    }

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeSwitch.switchTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adv_settings)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        // setTitle(R.string.app_name)

        // 显示返回按钮
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        spf = getSharedPreferences("adv", Context.MODE_PRIVATE)

        adv_retrieve_window.setOnClickListener {
            spf.edit().putBoolean("adv_retrieve_window", (it as Switch).isChecked).commit()
        }
        adv_keyevent.setOnClickListener {
            spf.edit().putBoolean("adv_keyevent", (it as Switch).isChecked).commit()
        }
        adv_find_viewid.setOnClickListener {
            spf.edit().putBoolean("adv_find_viewid", (it as Switch).isChecked).commit()
        }
        adv_event_window_state.setOnClickListener {
            spf.edit().putBoolean("adv_event_window_state", (it as Switch).isChecked).commit()
        }
        adv_event_content_change.setOnClickListener {
            spf.edit().putBoolean("adv_event_content_change", (it as Switch).isChecked).commit()
        }
        adv_event_view_click.setOnClickListener {
            spf.edit().putBoolean("adv_event_view_click", (it as Switch).isChecked).commit()
        }
        adv_event_usage_stats.setOnClickListener {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (e: ActivityNotFoundException) {
                val item = (it as Switch)
                item.isChecked = !item.isChecked
            }
        }
    }

    fun checkAppUsagePermission(): Boolean {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager?
        if (usageStatsManager == null) {
            return false;
        }
        val currentTime = System.currentTimeMillis()
        // try to get app usage state in last 1 min
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 60 * 1000, currentTime);
        if (stats.size == 0) {
            return false;
        }

        return true;
    }

    override fun onResume() {
        super.onResume()

        adv_retrieve_window.isChecked = spf.getBoolean("adv_retrieve_window", true)
        adv_keyevent.isChecked = spf.getBoolean("adv_keyevent", true)
        adv_find_viewid.isChecked = spf.getBoolean("adv_find_viewid", true)
        adv_event_window_state.isChecked = spf.getBoolean("adv_event_window_state", true)
        adv_event_content_change.isChecked = spf.getBoolean("adv_event_content_change", false)
        adv_event_view_click.isChecked = spf.getBoolean("adv_event_view_click", false)
        adv_event_usage_stats.isChecked = checkAppUsagePermission()
    }

    public override fun onPause() {
        super.onPause()
    }
}
