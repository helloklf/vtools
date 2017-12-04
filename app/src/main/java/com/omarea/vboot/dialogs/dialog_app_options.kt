package com.omarea.vboot.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import com.omarea.shell.SuDo
import com.omarea.vboot.R
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by helloklf on 2017/12/04.
 */

class dialog_app_options(internal var context: Context, internal var apps: ArrayList<HashMap<String, Any>>) {
    var allowPigz = false
    var backupPath = "/sdcard/Android/apps/"
    lateinit var next: Runnable

    fun selectOptions(next: Runnable) {
        options()
        this.next = next
    }

    private fun options() {
        val layoutInflater = LayoutInflater.from(context)
        val dialog = layoutInflater.inflate(R.layout.dialog_app_options, null)
        val alert = AlertDialog.Builder(context).setTitle("请选择操作")
                .setView(dialog)
                .setCancelable(false)
                /*
                .setItems(
                        arrayOf("备份",
                                "还原",
                                "卸载",
                                "清空数据",
                                "清缓存",
                                "冻结",
                                "解冻"), { dialog, which ->
                    optionNext(which)
                })
                */
                .show()
    }

    private fun optionNext(which: Int) {
        when (which) {
            0 -> backupAll()
            1 -> restoreAll()
            2 -> uninstallAll()
            3 -> clearAll()
            4 -> trimCachesAll()
            5 -> disableAll()
            6 -> enableAll()
        }
        if (this.next != null) {
            this.next.run()
        }
    }

    private fun backupAll() {
        if (File("/system/xbin/pigz").exists() || File("/system/bin/pigz").exists()) {
            allowPigz = true
        }

        var sb = StringBuilder()

        for (item in apps) {
            val packageName = item.get("packageName").toString()
            val path = item.get("path").toString()

            sb.append("cd /data/data/$packageName;")
            sb.append("cp $path $backupPath$packageName.apk;")
            if (allowPigz)
                sb.append("tar cf - * --exclude ./cache --exclude ./lib | pigz > $backupPath$packageName.tar.gz;")
            else
                sb.append("tar -czf $backupPath$packageName.tar.gz * --exclude ./cache --exclude ./lib;")
            sb.append("echo Y\n")
        }

        SuDo(context).execCmdSync(sb.toString())
    }

    private fun restoreAll() {
        var sb = StringBuilder()
        for (item in apps) {
            val packageName = item.get("packageName").toString()
            if (File("$backupPath$packageName.tar.gz").exists()) {
                sb.append("pm install $backupPath$packageName.tar.gz;")
                sb.append("pm clear $packageName;")
                sb.append("cd /data/data/$packageName;")
                sb.append("tar -xzf $backupPath$packageName.tar.gz;")
                sb.append("echo Y\n")
            }
        }

        SuDo(context).execCmdSync(sb.toString())
    }

    private fun disableAll() {
        var sb = StringBuilder()
        for (item in apps) {
            val packageName = item.get("packageName").toString()
            sb.append("pm disable $packageName;")
        }

        SuDo(context).execCmdSync(sb.toString())
    }

    private fun enableAll() {
        var sb = StringBuilder()
        for (item in apps) {
            val packageName = item.get("packageName").toString()
            sb.append("pm enable $packageName;")
        }

        SuDo(context).execCmdSync(sb.toString())
    }

    private fun clearAll() {
        var sb = StringBuilder()
        for (item in apps) {
            val packageName = item.get("packageName").toString()
            sb.append("pm clear $packageName;")
        }

        SuDo(context).execCmdSync(sb.toString())
    }

    private fun trimCachesAll() {
        var sb = StringBuilder()
        for (item in apps) {
            val packageName = item.get("packageName").toString()
            sb.append("pm trim-caches $packageName;")
        }

        SuDo(context).execCmdSync(sb.toString())
    }

    private fun uninstallAll() {
        var sb = StringBuilder()
        for (item in apps) {
            val packageName = item.get("packageName").toString()
            sb.append("pm uninstall $packageName;")
        }

        SuDo(context).execCmdSync(sb.toString())
    }
}
