package com.omarea.vtools

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.widget.Switch
import com.omarea.shared.SpfConfig
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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adv_settings)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

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
    }

    override fun onResume() {
        super.onResume()

        adv_retrieve_window.isChecked = spf.getBoolean("adv_retrieve_window", true)
        adv_keyevent.isChecked = spf.getBoolean("adv_keyevent", true)
        adv_find_viewid.isChecked = spf.getBoolean("adv_find_viewid", true)
    }

    public override fun onPause() {
        super.onPause()
    }
}
