package com.omarea.library.basic

import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

/**
 * 获取系统的输入法
 * Created by Hello on 2018/01/23.
 */

internal class InputMethodApp(private var context: Context) {
    /**
     * 获取系统已安装的输入法
     */
    fun getInputMethods(): ArrayList<String> {
        // settings get secure enabled_input_methods
        try {
            val enable = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ENABLED_INPUT_METHODS)
            if (!enable.isNullOrEmpty()) {
                val list = arrayListOf<String>()
                enable.split(":").map {
                    it.split("/").first()
                }.toCollection(list)
                return list
            }
        } catch (ex: Exception) {
        }

        val ignoredList = arrayListOf<String>()
        val im = (context.getSystemService(Context.INPUT_METHOD_SERVICE)) as InputMethodManager?
        return if (im == null) {
            ignoredList
        } else {
            val inputList = im.inputMethodList
            for (input in inputList) {
                ignoredList.add(input.packageName)
            }
            ignoredList
        }
    }
}
