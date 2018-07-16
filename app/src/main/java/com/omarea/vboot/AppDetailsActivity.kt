package com.omarea.vboot

import android.os.Bundle
import android.app.Activity

import kotlinx.android.synthetic.main.activity_app_details.*

class AppDetailsActivity(appPackageName: String) : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_details)
    }
}
