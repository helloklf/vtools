package com.omarea.vtools.addin

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
import com.omarea.shared.CommonCmds
import com.omarea.shell.AsynSuShellUnit
import com.omarea.shell.Props
import com.omarea.vtools.R
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

    open class ProgressHandler(context: Context) : Handler() {
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
                    } else if (Regex("^\\[.*]\$").matches(obj)) {
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

        val arr = arrayOf("Speed编译(推荐）", "强制Speed编译", "Everything编译", "强制Everything编译", "Reset", "按默认方式优化（Oreo+）")
        var index = 0
        AlertDialog.Builder(context)
                .setTitle("请选择执行方式")
                .setSingleChoiceItems(arr, index, { _, which ->
                    index = which
                })
                .setNegativeButton("确定", { _, _ ->
                    if (index == arr.size - 1) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                            Toast.makeText(context, "系统版本过低（需要Android 8.0+），不支持！", Toast.LENGTH_SHORT).show()
                            return@setNegativeButton
                        } else {
                            val commands = StringBuilder()
                            commands.append("cmd package bg-dexopt-job")
                            commands.append("\n\n")
                            commands.append("echo '[operation completed]';")
                            commands.append("\n\n")
                            AsynSuShellUnit(ProgressHandler(context)).exec(commands.toString()).waitFor()
                        }
                    } else {
                        val apps = getAllPackageNames()
                        val commands = StringBuilder()
                        val action = if (index == 4) "reset" else "compile"
                        for (app in apps) {
                            commands.append("echo '[${action} ${app}]'")
                            commands.append(";\n`")
                            when (index) {
                                0 -> commands.append("cmd package compile -m speed ${app}")
                                1 -> commands.append("cmd package compile -m speed -f ${app}")
                                2 -> commands.append("cmd package compile -m everything ${app}")
                                3 -> commands.append("cmd package compile -m everything -f ${app}")
                                4 -> commands.append("cmd package compile --reset ${app}")
                            }
                            commands.append("` > /dev/null;\n\n")
                        }
                        commands.append("echo '[operation completed]';")
                        commands.append("\n\n")
                        when (index) {
                            0 -> commands.append("cmd package compile -m speed ${context.packageName}")
                            1 -> commands.append("cmd package compile -m speed -f ${context.packageName}")
                            2 -> commands.append("cmd package compile -m everything ${context.packageName}")
                            3 -> commands.append("cmd package compile -m everything -f ${context.packageName}")
                            4 -> commands.append("cmd package compile --reset ${context.packageName}")
                        }
                        commands.append("\n\n")
                        AsynSuShellUnit(ProgressHandler(context)).exec(commands.toString()).waitFor()
                    }
                })
                .setNeutralButton("查看说明", { _, _ ->
                    AlertDialog.Builder(context).setTitle("说明").setMessage("在Android N以后，为了减少应用程序空间占用和提高安装效率，引入了新的机制。在安装应用时，不再像6.0时代一样将整个应用编译成本地代码，通过cmd package compile命令，可用于手动触发编译，常用以下几种模式：\n\nSpeed：尽可能的提高运行效率\nEverything：编译可以被编译的一切\nReset命令用于清除配置脚本和已编译过的代码\n选择强制编译时，将重新编译已经编译过的应用。\nReset命令用于重置所有应用的Dex编译状态。\n\n 以8.0系统下斗鱼TV客户端为例，全新安装时base.odex仅为6.8MB，使用Speed模式编译后，base.odex文件增大到103MB。\n\n由于国内许多应用均使用了热更新技术，或使用其它自定义引擎（Weex、React等）来提高开发效率，但需要在运行时才解析并生成原生组件来渲染，甚至功能代码被托管在服务器端（每次运行都可能需要重新下载并解析）。这是导致应用启动慢或启动后卡顿的主要原因。因此Speed、Everything模式编译均不能为这类应用带来明显的性能提升！").setNegativeButton("了解更多", { dialog, which ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://source.android.com/devices/tech/dalvik/jit-compiler?hl=zh-cn")))
                    }).create().show()
                })
                .create().show()
    }

    override fun run() {
        run2()
    }

    fun modifyConfigOld () {
        val arr = arrayOf(
                "verify",
                "speed",
                "恢复默认")
        val intallMode = Props.getProp("dalvik.vm.dex2oat-filter")
        var index = 0
        when (intallMode) {
            "interpret-only" -> index = 0
            "speed" -> index = 1
        }
        AlertDialog.Builder(context)
                .setTitle("请选择Dex2oat配置")
                .setSingleChoiceItems(arr, index, { _, which ->
                    index = which
                })
                .setNegativeButton("确定", { _, _ ->
                    val stringBuilder = StringBuilder()

                    //移除已添加的配置
                    stringBuilder.append("sed '/^dalvik.vm.image-dex2oat-filter=/'d /system/build.prop > /data/build.prop;")
                    stringBuilder.append("sed -i '/^dalvik.vm.dex2oat-filter=/'d /data/build.prop;")

                    when (index) {
                        0 -> {
                            stringBuilder.append("sed -i '\$adalvik.vm.image-dex2oat-filter=interpret-only' /data/build.prop;")
                            stringBuilder.append("sed -i '\$adalvik.vm.dex2oat-filter=interpret-only' /data/build.prop;")
                        }
                        1 -> {
                            stringBuilder.append("sed -i '\$adalvik.vm.image-dex2oat-filter=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$adalvik.vm.dex2oat-filter=speed' /data/build.prop;")
                        }
                        2 -> { }
                    }

                    stringBuilder.append(CommonCmds.MountSystemRW)
                    stringBuilder.append("cp /system/build.prop /system/build.prop.${System.currentTimeMillis()}\n")
                    stringBuilder.append("cp /data/build.prop /system/build.prop\n")
                    stringBuilder.append("rm /data/build.prop\n")
                    stringBuilder.append("chmod 0755 /system/build.prop\n")

                    execShell(stringBuilder)
                    Toast.makeText(context, "配置已修改，但需要重启才能生效！", Toast.LENGTH_SHORT).show()
                })
                .setNeutralButton("查看说明", { _, _ ->
                    AlertDialog.Builder(context).setTitle("说明").setMessage("interpret-only模式安装应用更快。speed模式安装应用将会很慢，但是运行速度更快。").create().show()
                })
                .create().show()
    }

    fun modifyConfig() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(context, "系统版本过低，至少需要Android 7.0！", Toast.LENGTH_SHORT).show()
            modifyConfigOld()
            return
        }

        val arr = arrayOf(
                "快速安装（闲时编译）",
                "Speed模式（尽量编译）",
                "Everything模式（完全编译）",
                "节省空间（永不编译）",
                "恢复默认")
        val intallMode = Props.getProp("pm.dexopt.install")
        var index = 0
        when (intallMode) {
            "extract" -> index = 0
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
                    //stringBuilder.append("sed '/^pm.dexopt.ab-ota=/'d /system/build.prop > /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.bg-dexopt=/'d /data/build.prop;")
                    //stringBuilder.append("sed -i '/^pm.dexopt.boot=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.core-app=/'d /data/build.prop;")
                    //stringBuilder.append("sed -i '/^pm.dexopt.first-boot=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.forced-dexopt=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.install=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.nsys-library=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^pm.dexopt.shared-apk=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^dalvik.vm.image-dex2oat-filter=/'d /data/build.prop;")
                    stringBuilder.append("sed -i '/^dalvik.vm.dex2oat-filter=/'d /data/build.prop;")

                    when (index) {
                        0 -> {
                            stringBuilder.append("sed -i '\$apm.dexopt.bg-dexopt=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.core-app=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.forced-dexopt=speed' /data/build.prop;")
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                stringBuilder.append("sed -i '\$apm.dexopt.install=interpret-only' /data/build.prop;")
                            } else {
                                stringBuilder.append("sed -i '\$apm.dexopt.install=quicken' /data/build.prop;")
                            }
                            stringBuilder.append("sed -i '\$apm.dexopt.nsys-library=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.shared-apk=speed' /data/build.prop;")
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                stringBuilder.append("sed -i '\$adalvik.vm.image-dex2oat-filter=speed' /data/build.prop;")
                                stringBuilder.append("sed -i '\$adalvik.vm.dex2oat-filter=speed' /data/build.prop;")
                            }
                        }
                        1 -> {
                            stringBuilder.append("sed -i '\$apm.dexopt.bg-dexopt=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.core-app=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.forced-dexopt=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.install=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.nsys-library=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.shared-apk=speed' /data/build.prop;")
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                stringBuilder.append("sed -i '\$adalvik.vm.image-dex2oat-filter=speed' /data/build.prop;")
                                stringBuilder.append("sed -i '\$adalvik.vm.dex2oat-filter=speed' /data/build.prop;")
                            }
                        }
                        2 -> {
                            stringBuilder.append("sed -i '\$apm.dexopt.bg-dexopt=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.core-app=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.forced-dexopt=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.install=everything' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.nsys-library=speed' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.shared-apk=speed' /data/build.prop;")
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                stringBuilder.append("sed -i '\$adalvik.vm.image-dex2oat-filter=speed' /data/build.prop;")
                                stringBuilder.append("sed -i '\$adalvik.vm.dex2oat-filter=speed' /data/build.prop;")
                            }
                        }
                        3 -> {
                            stringBuilder.append("sed -i '\$apm.dexopt.bg-dexopt=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.boot=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.core-app=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.first-boot=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.forced-dexopt=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.nsys-library=verify-none' /data/build.prop;")
                            stringBuilder.append("sed -i '\$apm.dexopt.shared-apk=verify-none' /data/build.prop;")
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                stringBuilder.append("sed -i '\$adalvik.vm.image-dex2oat-filter=verify-none' /data/build.prop;")
                                stringBuilder.append("sed -i '\$adalvik.vm.dex2oat-filter=verify-none' /data/build.prop;")
                            }
                        }
                        4 -> {
                            //
                        }
                    }

                    stringBuilder.append(CommonCmds.MountSystemRW)
                    stringBuilder.append("cp /system/build.prop /system/build.prop.${System.currentTimeMillis()}\n")
                    stringBuilder.append("cp /data/build.prop /system/build.prop\n")
                    stringBuilder.append("rm /data/build.prop\n")
                    stringBuilder.append("chmod 0755 /system/build.prop\n")

                    execShell(stringBuilder)
                    Toast.makeText(context, "配置已修改，但需要重启才能生效！", Toast.LENGTH_SHORT).show()
                })
                .setNeutralButton("查看说明", { _, _ ->
                    AlertDialog.Builder(context).setTitle("说明").setMessage("在Android N以后，为了减少应用程序空间占用和提高安装效率，引入了新的机制。在安装应用时，不再像6.0时代一样将整个应用编译成本地代码，仅在设备空闲时编译优化常用的代码块。\n\n我们可以改变这种策略，让PM程序在安装应用时编译更多内容，降低在运行时的CPU占用提高流畅度。\n\n建议修改后重启手机，并进行一次“强制编译Dex”操作！\n\n以8.0系统下斗鱼TV客户端为例，全新安装时base.odex仅为6.8MB，使用Speed模式编译后（Everything模式编码空间占用相近，但编译更慢），base.odex文件增大到103MB。\n\n由于国内许多应用均使用了热更新技术，或使用其它自定义引擎（Weex、React等）来提高开发效率，但需要在运行时才解析并生成原生组件来渲染，甚至功能代码被托管在服务器端（每次运行都可能需要重新下载并解析）。这是导致应用启动慢或启动后卡顿的主要原因。因此Speed、Everything模式编译均不能为这类应用带来明显的性能提升！").setNegativeButton("了解更多", { dialog, which ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://source.android.com/devices/tech/dalvik/configure?hl=zh-cn")))
                    }).create().show()
                })
                .create().show()
    }

    fun modifyThreadConfig() {
        /*
        [dalvik.vm.boot-dex2oat-threads]: [8]
        [dalvik.vm.dex2oat-threads]: [4]
        [dalvik.vm.image-dex2oat-threads]: [4]
        [ro.sys.fw.dex2oat_thread_count]: [4]
        */
    }
}
