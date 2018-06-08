package com.omarea.ui

import android.text.Editable
import android.text.TextWatcher

/**
 * 监听文本变化的简单SearchTextWatcher
 * Created by Hello on 2018/03/04.
 */


public class SearchTextWatcher(private var onChange: Runnable) : TextWatcher {
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        onChange.run()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun afterTextChanged(s: Editable?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}