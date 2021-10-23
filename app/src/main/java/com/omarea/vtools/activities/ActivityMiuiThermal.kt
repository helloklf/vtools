package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.KernelProrp
import com.omarea.common.shell.RootFile
import com.omarea.common.ui.DialogHelper
import com.omarea.library.device.MiuiThermalAESUtil
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_miui_thermal.*
import java.io.File
import java.nio.charset.Charset

class ActivityMiuiThermal : ActivityBase() {
    private val REQUEST_CFG_FILE = 1
    private var currentFile = ""
    private var encrypted = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_miui_thermal)

        setBackArrow()
        onViewCreated()
    }

    private fun onViewCreated() {
    }

    override fun onResume() {
        super.onResume()
        title = getString(R.string.menu_miui_thermal)
    }


    private fun openDir() {
        try {
            val options = applicationContext.resources.getTextArray(R.array.start_dir_options)
            var currentIndex = 0
            DialogHelper.animDialog(
                    AlertDialog.Builder(this)
                            .setTitle("选择配置来源目录")
                            .setSingleChoiceItems(options, currentIndex) { _, index ->
                                currentIndex = index
                            }.setPositiveButton("浏览选定目录") { _, _ ->
                                if (currentIndex > -1) {
                                    val intent = Intent(this.applicationContext, ActivityFileSelector::class.java)
                                    intent.putExtra("extension", "conf")
                                    intent.putExtra("start", options.get(currentIndex))
                                    startActivityForResult(intent, REQUEST_CFG_FILE)
                                }
                            })
        } catch (ex: Exception) {
            Toast.makeText(this, "启动内置文件选择器失败！", Toast.LENGTH_SHORT).show()
        }
        // Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    @SuppressLint("RestrictedApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CFG_FILE && data != null && data.extras != null) {
            val fileName = data.extras!!.getString("file");
            if (!fileName!!.startsWith("thermal")) {
                currentFile = fileName
                title = fileName

                try {
                    readConfig()
                    encrypted = true
                } catch (ex: Exception) {
                    val content = String(File(currentFile).readBytes(), Charset.forName("UTF-8")).trim()
                    thermal_config.setText(content)
                    encrypted = false
                    return
                }
            } else {
                Toast.makeText(this, "这个文件名，看上去不是一个温控配置文件~", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun readConfig() {
        val file = File(currentFile)
        val output = MiuiThermalAESUtil.decrypt(file.readBytes())
        thermal_config.setText(String(output, Charset.forName("UTF-8")))
        setTitle(file.name)
    }

    @SuppressLint("RestrictedApi")
    private fun saveConfig() {
        val currentContent = thermal_config.text.toString().trim().replace(Regex("\r\n"), "\n").replace(Regex("\r\t"), "\t")
        val bytes = currentContent.toByteArray(Charset.forName("UTF-8"))
        val data = if (encrypted) MiuiThermalAESUtil.encrypt(bytes) else bytes
        val file_path = filesDir.path + File.separator + "thermal-temp.conf"
        File(file_path).writeBytes(data)
        // TODO:
        val result = KeepShellPublic.doCmdSync(
                "busybox mount -o rw,remount /\n" +
                        "busybox mount -o rw,remount /system\n" +
                        "mount -o rw,remount /system\n" +
                        "busybox mount -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                        "mount -o remount,rw /dev/block/bootdevice/by-name/system /system\n" +
                        "busybox mount -o rw,remount /vendor\n" +
                        "mount -o rw,remount /vendor\n" +
                        "cp \"$file_path\" \"$currentFile\"\n" +
                        "chmod 664 \"$currentFile\""
        )
        File(file_path).delete()
        if (result == "error") {
            Toast.makeText(this, "保存失败，请检查是否已授予ROOT权限，以及文件是否被锁定！", Toast.LENGTH_LONG).show()
        } else {
            val output = (try {
                MiuiThermalAESUtil.decrypt(File(currentFile).readBytes())
            } catch (ex: Exception) {
                File(currentFile).readBytes()
            })
            val savedContent = String(output, Charset.forName("UTF-8"))
            if (savedContent.equals(currentContent)) {
                Toast.makeText(this, "保存成功~", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "保存失败，请检查是否已授予ROOT权限，以及文件是否被锁定！", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun applyThermal(saveConfig: Boolean) {
        val currentContent = thermal_config.text.toString().trim().replace(Regex("\r\n"), "\n").replace(Regex("\r\t"), "\t")
        val bytes = currentContent.toByteArray(Charset.forName("UTF-8"))
        val data = if (encrypted) MiuiThermalAESUtil.encrypt(bytes) else bytes
        val file_path = filesDir.path + File.separator + "thermal-temp.conf"
        val fileName = File(currentFile).name
        val outPath = "/data/vendor/thermal/config/$fileName"
        File(file_path).writeBytes(data)
        if (RootFile.dirExists("/data/vendor/thermal/config")) {
            // TODO:
            val result = KeepShellPublic.doCmdSync(
                    "cp \"$file_path\" \"$outPath\"\n" +
                            "chmod 664 \"$outPath\""
            )
            File(file_path).delete()
            if (result == "error") {
                Toast.makeText(this, "未能应用温控配置！", Toast.LENGTH_LONG).show()
            } else {
                val savedContent = KernelProrp.getProp("/data/vendor/thermal/decrypt.txt").trim()
                if (savedContent.equals(currentContent)) {
                    Toast.makeText(this, "应用温控配置成功~", Toast.LENGTH_LONG).show()
                } else if (!RootFile.fileExists(outPath)) {
                    Toast.makeText(this, "未能应用温控配置！", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(
                            this,
                            "无法确认温控是否应用成功，请检查日志或通过dump thermal-engine验证生效情况！",
                            Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            Toast.makeText(this, "系统不支持或目录已被破坏，无法应用温控配置！", Toast.LENGTH_LONG).show()
        }
        if (saveConfig) {
            saveConfig()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_miui_thermal, menu)
        return true
    }


    private fun openUrl(link: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (ex: Exception) {
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        return if (id == R.id.action_open) {
            openDir()
            true
        } else if (id == R.id.action_save) {
            if (currentFile.isNotEmpty()) {
                saveConfig()
            } else {
                Toast.makeText(this, "你都还没打开文件呢，保存个毛啊！", Toast.LENGTH_SHORT).show()
            }
            true
        } else if (id == R.id.action_apply) {
            if (currentFile.isNotEmpty()) {
                val view = layoutInflater.inflate(R.layout.dialog_apply_thermal, null)
                val dialog = DialogHelper.customDialog(this, view)
                view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
                    dialog.dismiss()
                }
                view.findViewById<View>(R.id.btn_applay).setOnClickListener {
                    val saveConfig = view.findViewById<CompoundButton>(R.id.save_thermal).isChecked
                    dialog.dismiss()
                    this.applyThermal(saveConfig)
                }
            } else {
                Toast.makeText(this, "你都还没打开文件呢，应用个毛啊！", Toast.LENGTH_SHORT).show()
            }
            true
        } else if (id == R.id.action_hele) {
            openUrl("https://github.com/helloklf/vtools/blob/scene3/docs/MIUI%E6%B8%A9%E6%8E%A7%E8%AF%B4%E6%98%8E.md")
            true
        } else super.onOptionsItemSelected(item)
    }
}
