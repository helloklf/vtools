package com.omarea.vtools.activities

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.omarea.vtools.R

class ActivityTimingTask : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeSwitch.switchTheme(this)

        setContentView(R.layout.activity_timing_task)
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onPause() {
        super.onPause()
    }
}