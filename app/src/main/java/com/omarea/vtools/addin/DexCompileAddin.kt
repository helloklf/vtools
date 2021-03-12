package com.omarea.vtools.addin

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.Toast
import com.omarea.common.ui.DialogHelper
import com.omarea.library.shell.PropsUtils
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R
import com.omarea.vtools.activities.ActivityBase
import com.omarea.vtools.services.CompileService

/**
 * Created by Hello on 2018/02/20.
 */

class DexCompileAddin(private var context: ActivityBase) : AddinBase(context) {
    fun isSupport(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(context, "系统版本过低，至少需要Android 7.0！", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun triggerCompile (action: String) {
        if (CompileService.compiling) {
            Toast.makeText(context, "有一个后台编译过程正在进行，不能重复开启", Toast.LENGTH_SHORT).show()
        } else {
            try {
                val service = Intent(context, CompileService::class.java)
                service.action = action
                context.startService(service)
                Toast.makeText(context, "开始后台编译，请查看通知了解进度", Toast.LENGTH_SHORT).show()
            } catch (ex: java.lang.Exception) {
                Toast.makeText(context, "启动后台过程失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //增加进度显示，而且不再出现因为编译应用自身而退出
    private fun run2() {
        if (!isSupport()) {
            return
        }

        if (CompileService.compiling) {
            Toast.makeText(context, "有一个后台编译过程正在进行~", Toast.LENGTH_SHORT).show()
            return
        }

        val view = context.layoutInflater.inflate(R.layout.dialog_addin_compile, null)
        val dialog = DialogHelper.customDialog(context, view)
        view.findViewById<View>(R.id.mode_speed_profile).setOnClickListener {
            dialog.dismiss()
            triggerCompile(context.getString(R.string.scene_speed_profile_compile))
        }
        view.findViewById<View>(R.id.mode_speed).setOnClickListener {
            dialog.dismiss()
            triggerCompile(context.getString(R.string.scene_speed_compile))
        }
        view.findViewById<View>(R.id.mode_everything).setOnClickListener {
            dialog.dismiss()
            triggerCompile(context.getString(R.string.scene_everything_compile))
        }
        view.findViewById<View>(R.id.mode_reset).setOnClickListener {
            dialog.dismiss()
            triggerCompile(context.getString(R.string.scene_reset_compile))
        }
        view.findViewById<View>(R.id.faq).setOnClickListener {
            dialog.dismiss()
            Toast.makeText(context, "此页面，在国内(CN)可能需要“虚拟专用网络”才能正常访问", Toast.LENGTH_LONG).show()

            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.addin_dex2oat_helplink))))
        }
    }

    override fun run() {
        run2()
    }

    fun modifyConfigOld() {
        val arr = arrayOf(
                "verify",
                "speed",
                "恢复默认")
        val intallMode = PropsUtils.getProp("dalvik.vm.dex2oat-filter")
        var index = 0
        when (intallMode) {
            "interpret-only" -> index = 0
            "speed" -> index = 1
        }
        DialogHelper.animDialog(AlertDialog.Builder(context)
                .setTitle("请选择Dex2oat配置")
                .setSingleChoiceItems(arr, index) { _, which ->
                    index = which
                }
                .setNegativeButton("确定") { _, _ ->
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
                    }

                    stringBuilder.append(CommonCmds.MountSystemRW)
                    stringBuilder.append("cp /system/build.prop /system/build.prop.${System.currentTimeMillis()}\n")
                    stringBuilder.append("cp /data/build.prop /system/build.prop\n")
                    stringBuilder.append("rm /data/build.prop\n")
                    stringBuilder.append("chmod 0755 /system/build.prop\n")

                    execShell(stringBuilder)
                    Toast.makeText(context, "配置已修改，但需要重启才能生效！", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("查看说明") { _, _ ->
                    DialogHelper.animDialog(AlertDialog.Builder(context).setTitle("说明").setMessage("interpret-only模式安装应用更快。speed模式安装应用将会很慢，但是运行速度更快。"))
                })
    }

    fun modifyConfig() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            modifyConfigOld()
            return
        }

        val arr = arrayOf(
                "不编译（优化安装速度）",
                "编译（优化运行速度）",
                "恢复默认")
        val intallMode = PropsUtils.getProp("pm.dexopt.install")
        var index = 0
        when (intallMode) {
            "extract",
            "quicken",
            "interpret-only",
            "verify-none" -> index = 0
            "speed" -> index = 1
            "everything" -> index = 1
            else -> {
                if (PropsUtils.getProp("pm.dexopt.core-app") == "verify-none") {
                    index = 3
                } else
                    index = 0
            }
        }
        DialogHelper.animDialog(AlertDialog.Builder(context)

                .setSingleChoiceItems(arr, index) { _, which ->
                    index = which
                }
                .setNegativeButton("确定") { _, _ ->
                    val stringBuilder = StringBuilder()

                    //移除已添加的配置
                    stringBuilder.append("cp /system/build.prop /data/build.prop;")
                    //stringBuilder.append("sed -i '/^pm.dexopt.ab-ota=/'d /data/build.prop;")
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
                    }

                    stringBuilder.append(CommonCmds.MountSystemRW)
                    stringBuilder.append("cp /system/build.prop /system/build.prop.${System.currentTimeMillis()}\n")
                    stringBuilder.append("cp /data/build.prop /system/build.prop\n")
                    stringBuilder.append("rm /data/build.prop\n")
                    stringBuilder.append("chmod 0755 /system/build.prop\n")

                    execShell(stringBuilder)
                    Toast.makeText(context, "配置已修改，但需要重启才能生效！", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("查看说明") { _, _ ->
                    DialogHelper.animDialog(AlertDialog.Builder(context)
                            .setTitle("说明")
                            .setMessage(R.string.addin_dexopt_helpinfo)
                            .setNegativeButton("了解更多") { _, _ ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.addin_dex2oat_helplink))))
                            })
                })
    }
}
