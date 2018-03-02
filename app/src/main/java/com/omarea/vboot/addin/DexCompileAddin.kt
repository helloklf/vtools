package com.omarea.vboot.addin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.omarea.shared.Consts

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

    override fun run() {
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
                    AlertDialog.Builder(context).setTitle("说明").setMessage("在Android N以后，为了减少应用程序空间占用和提高安装效率，引入了新的机制。在安装应用时，不再像6.0时代一样将整个应用编译成本地代码，同时增加了cmd package compile命令，可用于手动触发编译。\n\nSpeed：尽可能的提高运行效率\nEverything：编译可以被编译的一切\n\nReset命令用于清除配置文件和已编译过的代码\n\n选择强制编译时，将重新编译已经编译过的应用").setNegativeButton("了解更多", { dialog, which ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://source.android.com/devices/tech/dalvik/jit-compiler?hl=zh-cn")))
                    }).create().show()
                })
                .create().show()
    }

    fun modifyConfig() {
        if (!isSupport()) {
            return
        }

        val stringBuilder = StringBuilder()

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

        stringBuilder.append(Consts.MountSystemRW)
        stringBuilder.append("cp /system/build.prop /system/build.prop.${System.currentTimeMillis()}\n")
        stringBuilder.append("cp /data/build.prop /system/build.prop\n")
        stringBuilder.append("rm /data/build.prop\n")
        stringBuilder.append("chmod 0644 /system/build.prop\n")

        execShell(stringBuilder)
        Toast.makeText(context, "配置已修改，但需要重启才能生效！", Toast.LENGTH_SHORT).show()
    }
}
