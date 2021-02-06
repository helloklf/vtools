package com.omarea.krscript.ui

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.omarea.common.model.SelectItem
import com.omarea.common.ui.AdapterAppChooser
import com.omarea.common.ui.DialogAppChooser
import com.omarea.krscript.R
import com.omarea.krscript.model.ActionParamInfo

class ParamsAppChooserRender(private var actionParamInfo: ActionParamInfo, private var context: FragmentActivity) : DialogAppChooser.Callback {
    private val systemUiVisibility = context.window?.decorView?.systemUiVisibility
    private var darkMode = systemUiVisibility != null && (systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) == 0

    private lateinit var valueView: TextView
    private lateinit var nameView: TextView
    private lateinit var packages: ArrayList<AdapterAppChooser.AppInfo>

    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_app, null)
        valueView = layout.findViewById(R.id.kr_param_app_package)
        nameView = layout.findViewById(R.id.kr_param_app_name)

        setTextView()

        layout.findViewById<View>(R.id.kr_param_app_btn).setOnClickListener {
            openAppChooser()
        }
        nameView.setOnClickListener {
            openAppChooser()
        }

        valueView.tag = actionParamInfo.name

        return layout
    }

    private fun openAppChooser() {
        setSelectStatus()

        // TODO:深色模式、浅色模式
        DialogAppChooser(darkMode, packages, actionParamInfo.multiple, this).show(context.supportFragmentManager, "app-chooser")
    }

    private fun loadPackages(includeMissing: Boolean = false): List<AdapterAppChooser.AppInfo> {
        val pm = context.packageManager
        val filter = actionParamInfo.optionsFromShell?.map {
            it.value
        }

        val packages = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES).filter {
            filter == null || filter.contains(it.packageName)
        }

        val options = ArrayList(packages.map {
            AdapterAppChooser.AppInfo().apply {
                appName = "" + it.applicationInfo.loadLabel(pm)
                packageName = it.packageName
            }
        })

        // 是否包含丢失的应用程序
        if (includeMissing && actionParamInfo.optionsFromShell != null) {
            for (item in actionParamInfo.optionsFromShell!!) {
                if (options.filter { it.packageName == item.value }.isEmpty()) {
                    options.add(AdapterAppChooser.AppInfo().apply {
                        appName = "" + item.title
                        packageName = "" + item.value
                    })
                }
            }
        }

        return options
    }

    private fun setSelectStatus() {
        packages.forEach {
            it.selected = false
        }
        val currentValue = valueView.text
        if (actionParamInfo.multiple) {
            currentValue.split(actionParamInfo.separator).run {
                this.forEach {
                    val value = it
                    val app = packages.find { it.packageName == value }
                    if (app != null) {
                        app.selected = true
                    }
                }
            }
        } else {
            val current = packages.find { it.packageName == currentValue }
            val currentIndex = if (current != null) packages.indexOf(current) else -1
            if (currentIndex > -1) {
                packages.get(currentIndex).selected = true
            }
        }
    }

    // 设置界面显示和元素赋值
    private fun setTextView() {
        packages = ArrayList(loadPackages(actionParamInfo.type == "packages"))

        packages.run {
            val labels = map { it.appName }.toTypedArray()
            val values = map { it.packageName }.toTypedArray()
            if (actionParamInfo.multiple) {
                ActionParamsLayoutRender.getParamValues(actionParamInfo)?.run {
                    this.forEach {
                        val value = it
                        val app = packages.find { it.packageName == value }
                        if (app != null) {
                            app.selected = true
                        }
                    }
                }

                onConfirm((packages.filter { it.selected }))
            } else {
                // TODO: 这里有过多的数据包装盒解包，需要进行优化
                val validOptions = ArrayList(packages.map {
                    SelectItem().apply {
                        title = it.appName
                        value = it.packageName
                    }
                }.toList())

                val currentIndex = ActionParamsLayoutRender.getParamOptionsCurrentIndex(actionParamInfo, validOptions)
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

    override fun onConfirm(apps: List<AdapterAppChooser.AppInfo>) {
        if (actionParamInfo.multiple) {
            val values = apps.map { it.packageName }.joinToString(actionParamInfo.separator)
            val labels = apps.map { it.appName }.joinToString("，")
            valueView.text = values
            nameView.text = labels
        } else {
            val item = apps.firstOrNull()
            if (item == null) {
                valueView.text = ""
                nameView.text = ""
            } else {
                valueView.text = item.packageName
                nameView.text = item.appName
            }
        }
    }
}
