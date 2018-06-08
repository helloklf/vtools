package com.omarea.vboot.addin

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Message
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.omarea.shared.Consts
import com.omarea.shell.AsynSuShellUnit
import com.omarea.shell.Props
import com.omarea.vboot.R
import java.util.*

/**
 * Created by Hello on 2018/02/20.
 */

class DexCompileAddin(private var context: Context) : AddinBase(context) {
    fun isSupport(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(context, "系统版本过低，至少需要Android 7.0！", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun run1() {
        if (!isSupport()) {
            return
        }

        val arr = arrayOf("Speed编译", "Everything编译", "强制Speed编译", "强制Everything编译", "Reset")
        var index = 0
        AlertDialog.Builder(context)
                .setTitle("请选择执行方式")
                .setSingleChoiceItems(arr, index, { _, which ->
                    index = which
                })
                .setNegativeButton("确定", { _, _ ->
                    when (index) {
                        0 -> execShell("cmd package compile -m speed -a;")
                        1 -> execShell("cmd package compile -m everything -a;")
                        2 -> execShell("cmd package compile -m speed -a -f;")
                        3 -> execShell("cmd package compile -m everything -a -f;")
                        4 -> execShell("cmd package compile --reset -a;")
                    }
                })
                .setNeutralButton("查看说明", { _, _ ->
                    AlertDialog.Builder(context).setTitle("说明").setMessage("在Android N以后，为了减少应用程序空间占用和提高安装效率，引入了新的机制。在安装应用时，不再像6.0时代一样将整个应用编译成本地代码，同时增加了cmd package compile命令，可用于手动触发编译。\n\nSpeed：尽可能的提高运行效率\nEverything：编译可以被编译的一切\n\nReset命令用于清除配置文件和已编译过的代码\n\n选择强制编译时，将重新编译已经编译过的应用。Reset命令用于重置所有应用的Dex编译状态。").setNegativeButton("了解更多", { dialog, which ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://source.android.com/devices/tech/dalvik/jit-compiler?hl=zh-cn")))
                    }).create().show()
                })
                .create().show()
    }


    class ProgressHandler(context: Context) : Handler() {
        protected var dialog: View
        protected var alert: android.app.AlertDialog
        protected var textView: TextView
        protected var progressBar: ProgressBar

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.obj != null) {
                if (msg.what == 0) {
                    textView.text = "正在执行操作..."
                } else {
                    val obj = msg.obj.toString()
                    if (obj == "[operation completed]") {
                        progressBar.progress = 100
                        textView.text = "操作完成！"
                        this.postDelayed({
                            alert.dismiss()
                            alert.hide()
                        }, 2000)
                    } else if (Regex("^\\[.*\\]\$").matches(obj)) {
                        progressBar.progress = msg.what
                        val txt = obj
                                .replace("[compile ", "[编译 ")
                                .replace("[reset ", "[重置 ")
                        textView.text = txt
                    }
                }
            }
        }

        init {
            val layoutInflater = LayoutInflater.from(context)
            dialog = layoutInflater.inflate(R.layout.dialog_app_options, null)
            alert = android.app.AlertDialog.Builder(context).setView(dialog).setCancelable(false).create()
            textView = (dialog.findViewById(R.id.dialog_app_details_pkgname) as TextView)
            progressBar = (dialog.findViewById(R.id.dialog_app_details_progress) as ProgressBar)
            alert.show()
            textView.text = "正在获取权限"
        }
    }


    private fun getAllPackageNames(): ArrayList<String> {
        val packageManager: PackageManager = context.packageManager
        val packageInfos = packageManager.getInstalledApplications(0)
        val list = ArrayList<String>()/*在数组中存放数据*/
        for (i in packageInfos.indices) {
            list.add(packageInfos[i].packageName)
        }
        list.remove(context.packageName)
        return list
    }

    //增加进度显示，而且不再出现因为编译应用自身而退出
    private fun run2() {
        if (!isSupport()) {
            return
        }

        val arr = arrayOf("Speed编译", "Everything编译", "强制Speed编译", "强制Everything编译", "Reset")
        var index = 0
        AlertDialog.Builder(context)
                .setTitle("请选择执行方式")
                .setSingleChoiceItems(arr, index, { _, which ->
                    index = which
                })
                .setNegativeButton("确定", { _, _ ->
                    val apps = getAllPackageNames()
                    val commands = StringBuilder()
                    val action = if (index == 4) "reset" else "compile"
                    for (app in apps) {
                        commands.append("echo '[${action} ${app}]'")
                        commands.append(";\n`")
                        when (index) {
                            0 -> commands.append("cmd package compile -m speed ${app}")
                            1 -> commands.append("cmd package compile -m everything ${app}")
                            2 -> commands.append("cmd package compile -m speed -f ${app}")
                            3 -> commands.append("cmd package compile -m everything -f ${app}")
                            4 -> commands.append("cmd package compile --reset ${app}")
                        }
                        commands.append("` > /dev/null;\n\n")
                    }
                    commands.append("echo '[operation completed]';")
                    commands.append("\n\n")
                    when (index) {
                        0 -> commands.append("cmd package compile -m speed ${context.packageName}")
                        1 -> commands.append("cmd package compile -m everything ${context.packageName}")
                        2 -> commands.append("cmd package compile -m speed -f ${context.packageName}")
                        3 -> commands.append("cmd package compile -m everything -f ${context.packageName}")
                        4 -> commands.append("cmd package compile --reset ${context.packageName}")
                    }
                    commands.append("\n\n")
                    AsynSuShellUnit(ProgressHandler(context)).exec(commands.toString()).waitFor()
                })
                .setNeutralButton("查看说明", { _, _ ->
                    AlertDialog.Builder(context).setTitle("说明").setMessage("在Android N以后，为了减少应用程序空间占用和提高安装效率，引入了新的机制。在安装应用时，不再像6.0时代一样将整个应用编译成本地代码，同时增加了cmd package compile命令，可用于手动触发编译。\n\nSpeed：尽可能的提高运行效率\nEverything：编译可以被编译的一切\n\nReset命令用于清除配置文件和已编译过的代码\n\n选择强制编译时，将重新编译已经编译过的应用。Reset命令用于重置所有应用的Dex编译状态。").setNegativeButton("了解更多", { dialog, which ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://source.android.com/devices/tech/dalvik/jit-compiler?hl=zh-cn")))
                    }).create().show()
                })
                .create().show()
    }

    override fun run() {
        run2()
    }

    fun modifyConfig() {
        if (!isSupport()) {
            return
        }

        val arr = arrayOf("最快安装速度", "最佳性能（Speed）", "完整编译（Everything）", "永不编译", "恢复默认")
        val intallMode = Props.getProp("pm.dexopt.install")
        var index = 0
        when (intallMode) {
            "speed" -> index = 1
            "everything" -> index = 2
            else -> {
                if (Props.getProp("pm.dexopt.core-app") == "verify-none") {
                    index = 3
                } else
                    index = 0
            }
        }
        AlertDialog.Builder(context)
                .setTitle("请选择pm.dexopt策略")
                .setSingleChoiceItems(arr, index, { _, which ->
                    index = which
                })
                .setNegativeButton("确定", { _, _ ->
                    val stringBuilder = StringBuilder()

                    //移除已添加的配置
                    stringBuilder.append("sed '/^pm.dexopt.ab-ota=/'d /system/build.prop > /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.bg-dexopt=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.boot=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.core-app=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.first-boot=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.forced-dexopt=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.install=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.nsys-library=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.shared-apk=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^dalvik.vm.image-dex2oat-filter=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^dalvik.vm.dex2oat-filter=/'d /data/build.prop;")

                    when (index) {
                        0 -> {
                            stringBuilder.append("sed -i '\$apm.dexopt.ab-ota=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.bg-dexopt=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.boot=verify-profile' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.core-app=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.first-boot=interpret-only' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.forced-dexopt=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.ab-ota=verify-none' /data/build.prop;")
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                stringBuilder.append("sed -i '\$apm.dexopt.ab-ota=interpret-only' /data/build.prop;")
                            } else {
                                stringBuilder.append("sed -i '\$apm.dexopt.ab-ota=quicken' /data/build.prop;")
                            }
                            stringBuilder.append("sed -i '\$apm.dexopt.nsys-library=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.shared-apk=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$adalvik.vm.image-dex2oat-filter=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$adalvik.vm.dex2oat-filter=speed' /data/build.prop;")
                        }
                        1 -> {
                            stringBuilder.append("sed -i '\$apm.dexopt.ab-ota=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.bg-dexopt=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.boot=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.core-app=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.first-boot=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.forced-dexopt=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.install=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.nsys-library=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.shared-apk=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$adalvik.vm.image-dex2oat-filter=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$adalvik.vm.dex2oat-filter=speed' /data/build.prop;")
                        }
                        2 -> {
                            stringBuilder.append("sed -i '\$apm.dexopt.ab-ota=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.bg-dexopt=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.boot=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.core-app=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.first-boot=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.forced-dexopt=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.install=everything' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.nsys-library=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.shared-apk=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$adalvik.vm.image-dex2oat-filter=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$adalvik.vm.dex2oat-filter=speed' /data/build.prop;")
                        }
                        3 -> {
                            stringBuilder.append("sed -i '\$apm.dexopt.ab-ota=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.bg-dexopt=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.boot=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.core-app=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.first-boot=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.forced-dexopt=verify-none' /data/build.prop;")
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                stringBuilder.append("sed -i '\$apm.dexopt.ab-ota=verify-none' /data/build.prop;")
                            } else {
                                stringBuilder.append("sed -i '\$apm.dexopt.ab-ota=quicken' /data/build.prop;")
                            }
                            stringBuilder.append("sed -i '\$apm.dexopt.nsys-library=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.shared-apk=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$adalvik.vm.image-dex2oat-filter=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$adalvik.vm.dex2oat-filter=verify-none' /data/build.prop;")
                        }
                        4 -> {
                            //
                        }
                    }

                    stringBuilder.append(Consts.MountSystemRW)
                    stringBuilder.append("cp /system/build.prop /system/build.prop.${System.currentTimeMillis()}\n")
                    stringBuilder.append("cp /data/build.prop /system/build.prop\n")
                    stringBuilder.append("rm /data/build.prop\n")
                    stringBuilder.append("chmod 0644 /system/build.prop\n")

                    execShell(stringBuilder)
                    Toast.makeText(context, "配置已修改，但需要重启才能生效！", Toast.LENGTH_SHORT).show()
                })
                .setNeutralButton("查看说明", { _, _ ->
                    AlertDialog.Builder(context).setTitle("说明").setMessage("在Android N以后，为了减少应用程序空间占用和提高安装效率，引入了新的机制。在安装应用时，不再像6.0时代一样将整个应用编译成本地代码，仅在设备空闲时编译优化常用的代码块。\n\n我们可以改变这种策略，让PM程序在安装应用时编译更多内容，降低在运行时的CPU占用提高流畅度。\n\n建议修改后重启手机，并进行一次“强制编译Dex”操作！\n\n注意：很不推荐使用“永不编译”模式，除非你的手机真的非常缺存储空间，而且你不关心运行速度！").setNegativeButton("了解更多", { dialog, which ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://source.android.com/devices/tech/dalvik/configure?hl=zh-cn")))
                    }).create().show()
                })
                .create().show()
    }
}
