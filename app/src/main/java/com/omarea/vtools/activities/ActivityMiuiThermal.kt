package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.omarea.common.shell.KeepShellPublic
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
                            .setSingleChoiceItems(options, currentIndex) { dialog, index ->
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
        val currentContent = thermal_config.text.toString().trim()
        val bytes = currentContent.toByteArray(Charset.forName("UTF-8"))
        val data = if (encrypted) MiuiThermalAESUtil.encrypt(bytes) else bytes
        val file_path = filesDir.path + File.separator + "temp.conf"
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
        } else if (id == R.id.action_hele) {
            openUrl("https://github.com/helloklf/vtools/blob/scene3/docs/MIUI%E6%B8%A9%E6%8E%A7%E8%AF%B4%E6%98%8E.md")
            true
        } else super.onOptionsItemSelected(item)
    }
}
