package com.omarea.vboot

import android.os.Bundle
import android.app.Activity

import kotlinx.android.synthetic.main.activity_app_details.*

class AppDetailsActivity : Activity {
    constructor(){
        
    }
    constructor(appPackageName: String) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_details)
    }
}
