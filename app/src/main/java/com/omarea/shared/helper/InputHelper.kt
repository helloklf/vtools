package com.omarea.shared.helper

import android.content.Context
import android.view.inputmethod.InputMethodManager

/**
 * 获取系统的输入法
 * Created by Hello on 2018/01/23.
 */

internal class InputHelper(private var context: Context) {
    fun getInputMethods(): ArrayList<String> {
        val ignoredList = arrayListOf<String>()
        val im = (context.getSystemService(Context.INPUT_METHOD_SERVICE)) as InputMethodManager?
        if (im == null) {
            return ignoredList
        }
        val inputList = im.inputMethodList
        for (input in inputList) {
            ignoredList.add(input.packageName)
        }
        return ignoredList
    }
}
