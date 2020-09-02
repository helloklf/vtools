package com.omarea.library.basic

import android.content.Context
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
