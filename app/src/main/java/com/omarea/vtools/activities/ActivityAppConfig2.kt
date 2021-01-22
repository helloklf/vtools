package com.omarea.vtools.activities

import android.os.Bundle
import com.omarea.ui.TabIconHelper2
import com.omarea.vtools.R
import com.omarea.vtools.fragments.FragmentSceneApps
import com.omarea.vtools.fragments.FragmentSceneSettings
import kotlinx.android.synthetic.main.activity_app_config2.*


class ActivityAppConfig2 : ActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_config2)

        setBackArrow()

        val tabIconHelper2 = TabIconHelper2(tab_list, tab_content, this, supportFragmentManager)
        tabIconHelper2.newTabSpec("应用", getDrawable(R.drawable.tab_app)!!, FragmentSceneApps())
        tabIconHelper2.newTabSpec("设置", getDrawable(R.drawable.tab_settings)!!, FragmentSceneSettings())
        tab_content.adapter = tabIconHelper2.adapter
    }

    override fun onResume() {
        super.onResume()
        title = getString(R.string.menu_scene_mode)
    }
}
