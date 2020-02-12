package com.omarea.krscript

import android.content.Context
import android.content.Intent
import android.widget.Toast

class TryOpenActivity(private val context:  Context, private val activity:String) {
    fun getIntent(): Intent? {
        try {
            val intent = if (activity.contains("/")) (Intent(Intent.ACTION_VIEW).apply {
                val info = activity.split("/")
                val packageName = info.first()
                val className = info.last()
                setClassName(packageName, if (className.startsWith(".")) (packageName + className) else className)
            }) else {
                Intent(activity)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        } catch (ex: java.lang.Exception) {
            return null
        }
    }
    fun tryOpen(): Boolean {
        try {
            context.startActivity(getIntent())
            return true
        } catch (ex: Exception) {
            Toast.makeText(context, context.getString(R.string.kr_slice_activity_fail), Toast.LENGTH_SHORT).show()
            return false
        }
    }
}
