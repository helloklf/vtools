package com.omarea.krscript.ui

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.SimpleAdapter
import android.widget.Spinner
import android.widget.TextView
import com.omarea.common.ui.DialogHelper
import com.omarea.krscript.R
import com.omarea.krscript.model.ActionParamInfo
import java.lang.StringBuilder

class ParamsAppChooserRender(private var actionParamInfo: ActionParamInfo, private var context: Context) {
    private var options: ArrayList<HashMap<String, Any>>? = null
    private var status = booleanArrayOf() // 多选状态
    private var labels: Array<String?> = arrayOf()
    private var values: Array<String?> = arrayOf()
    private var currentIndex:Int = -1 // 单选状态

    fun render(): View {
        options = actionParamInfo.optionsFromShell
        val packages =  ArrayList(loadPackages(actionParamInfo.type == "packages"))
        val validOptions = ArrayList(packages.map {
            HashMap<String, Any>().apply {
                put("title", "" + it.desc)
                put("item", it)
            }
        }.toList())

        packages.run {
            labels = map { it.desc }.toTypedArray()
            values = map { it.value }.toTypedArray()
            if (actionParamInfo.multiple) {
                status = ActionParamsLayoutRender.getParamOptionsSelectedStatus(actionParamInfo, validOptions)
            } else {
                currentIndex = ActionParamsLayoutRender.getParamOptionsCurrentIndex(actionParamInfo, validOptions)
            }
        }

        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_app, null)
        val valueView = layout.findViewById<TextView>(R.id.kr_param_app_package)
        val nameView = layout.findViewById<TextView>(R.id.kr_param_app_name)

        updateTextView(valueView, nameView)

        layout.findViewById<ImageButton>(R.id.kr_param_app_btn).setOnClickListener {
            openDialog(valueView, nameView)
        }
        nameView.setOnClickListener {
            openDialog(valueView, nameView)
        }

        valueView.tag = actionParamInfo.name

        return layout
    }

    private fun loadPackages(includeMissing: Boolean = false): List<ActionParamInfo.ActionParamOption> {
        val pm = context.packageManager
        val filter = actionParamInfo.optionsFromShell?.map {
            (it.get("item") as ActionParamInfo.ActionParamOption).value
        }

        val packages = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES).filter {
            filter == null || filter.contains(it.packageName)
        }

        val options = ArrayList(packages.map {
            ActionParamInfo.ActionParamOption().apply {
                desc = "" + it.applicationInfo.loadLabel(pm)
                value = it.packageName
            }
        })

        // 是否包含丢失的应用程序
        if (includeMissing && actionParamInfo.optionsFromShell != null) {
            for (item in actionParamInfo.optionsFromShell!!.map { (it.get("item") as ActionParamInfo.ActionParamOption) }) {
                if (options.filter { it.value == item.value }.size < 1) {
                    options.add(ActionParamInfo.ActionParamOption().apply {
                        desc = item.desc
                        value = item.value
                    })
                }
            }
        }

        return options
    }

    private fun openDialog(valueView: TextView, nameView: TextView) {
        if (actionParamInfo.multiple) {
            DialogHelper.animDialog(
                    AlertDialog.Builder(context)
                            .setTitle("请选择应用")
                            .setMultiChoiceItems(
                                    labels,
                                    status
                            ) { dialog, index, isChecked ->
                                status[index] = isChecked
                            }
                            .setNeutralButton(R.string.btn_cancel) { _,_ ->
                            }
                            .setPositiveButton(R.string.btn_confirm) { _,_ ->
                                updateTextView(valueView, nameView)
                            }
            )
        } else {
            DialogHelper.animDialog(
                    AlertDialog.Builder(context)
                            .setTitle(R.string.kr_please_choose_app)
                            .setSingleChoiceItems(
                                    labels,
                                    currentIndex
                            )
                            { dialog, index ->
                                currentIndex = index
                                updateTextView(valueView, nameView)

                                dialog.dismiss()
                            })
        }
    }

    // 设置界面显示和元素赋值
    private fun updateTextView(valueView: TextView, nameView: TextView) {
        if (actionParamInfo.multiple) {
            val valuesResult = ArrayList<String>()
            val lablesResult = ArrayList<String>()
            for(i in 0 until status.size) {
                if (status[i]) {
                    valuesResult.add(values[i]!!)
                    lablesResult.add(labels[i]!!)
                }
            }
            valueView.text = valuesResult.joinToString(actionParamInfo.separator)
            nameView.text = lablesResult.joinToString("，")
        } else {
            if (currentIndex > -1) {
                valueView.text = values[currentIndex]
                nameView.text = labels[currentIndex]
            } else {
                valueView.text = ""
                nameView.text = ""
            }
        }
    }
}
