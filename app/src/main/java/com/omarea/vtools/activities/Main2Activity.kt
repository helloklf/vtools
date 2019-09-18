package com.omarea.vtools.activities

import android.os.Bundle
import android.preference.*

class Main2Activity : PreferenceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferenceScreen = this.getPreferenceManager().createPreferenceScreen(this)
        this.setPreferenceScreen(preferenceScreen)
        createPreferenceGroup(preferenceScreen)
    }

    private fun createPreference(preferenceCategory: PreferenceCategory){
        val checkBoxPreference = SwitchPreference(this)
        checkBoxPreference.key = "key_aaaa"//设置key
        checkBoxPreference.title = "标题"
        checkBoxPreference.summary = "标题下的小字"

        preferenceCategory.addPreference(checkBoxPreference)
    }

    private fun createPreferenceGroup(preferenceScreen: PreferenceScreen) {
        val preferenceCategory = PreferenceCategory(this)
        preferenceCategory.title = "分组标题"

        preferenceScreen.addPreference(preferenceCategory)

        createPreference(preferenceCategory)
    }
}
