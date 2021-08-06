package com.omarea.vtools.dialogs

import android.app.Activity
import android.content.Context
import android.view.View
import com.omarea.common.ui.DialogHelper
import com.omarea.vtools.R

class DialogAppCGroupMem(var context: Activity, val current: String?, val iResultCallback: IResultCallback) {
    interface IResultCallback {
        fun onChange(group: String?, name: String?)
    }

    class Transform(private val context: Context) {
        private val res = context.resources
        private val groupNames = ArrayList<String>().apply {
            addAll(res.getStringArray(R.array.cgroup_mem_options))
        }
        private val groupValues = ArrayList<String>().apply {
            addAll(res.getStringArray(R.array.cgroup_mem_values))
        }

        fun getName(group: String?): String {
            val selectedIndex = groupValues.indexOf(group)

            if (selectedIndex > -1) {
                return groupNames[selectedIndex]
            } else {
                return "Unknown"
            }
        }
    }

    private fun onItemClick (group: String?) {
        if (group != current) {
            iResultCallback.onChange(group, getName(group))
        }
    }

    fun getName(group: String?): String {
        return Transform(context).getName(group)
    }

    fun show() {
        val view = context.layoutInflater.inflate(R.layout.dialog_scene_app_cgroup, null)
        val dialog = DialogHelper.customDialog(context, view)

        view.findViewById<View>(R.id.cgroup_default).setOnClickListener {
            dialog.dismiss()
            onItemClick("")
        }
        view.findViewById<View>(R.id.cgroup_lock).setOnClickListener {
            dialog.dismiss()
            onItemClick("scene_lock")
        }
        view.findViewById<View>(R.id.cgroup_perf).setOnClickListener {
            dialog.dismiss()
            onItemClick("scene_perf")
        }
        view.findViewById<View>(R.id.cgroup_sys).setOnClickListener {
            dialog.dismiss()
            onItemClick("scene_fg")
        }
        view.findViewById<View>(R.id.cgroup_limit).setOnClickListener {
            dialog.dismiss()
            onItemClick("scene_bg")
        }
        view.findViewById<View>(R.id.cgroup_limit2).setOnClickListener {
            dialog.dismiss()
            onItemClick("scene_cache")
        }
    }
}
