package com.omarea.vtools.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.widget.AdapterView
import android.widget.SimpleAdapter
import android.widget.Toast
import com.omarea.Scene
import com.omarea.common.ui.DialogHelper
import com.omarea.shell_utils.BackupRestoreUtils
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_img.*
import java.io.File
import java.util.*


class ActivityImg : ActivityBase() {
    fun createItem(title: String, desc: String): HashMap<String, Any> {
        val item = HashMap<String, Any>()
        item.put("Title", title)
        item.put("Desc", desc)
        return item
    }

    //获取SD卡可用空间
    fun getSDFreeSizeMB(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.availableBytes / 1024 / 1024 //剩余空间
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_img)

        setBackArrow()

        onViewCreated()
    }

    override fun onResume() {
        super.onResume()
        title = getString(R.string.menu_img)
    }

    fun onViewCreated() {
        val listItem = ArrayList<HashMap<String, Any>>()/*在数组中存放数据*/

        listItem.add(createItem(getString(R.string.backup_action_title_boot), getString(R.string.backup_action_desc_boot)))
        listItem.add(createItem(getString(R.string.restore_action_title_boot), getString(R.string.restore_action_desc_boot)))

        listItem.add(createItem(getString(R.string.backup_action_title_rec), getString(R.string.backup_action_desc_rec)))
        listItem.add(createItem(getString(R.string.restore_action_title_rec), getString(R.string.restore_action_desc_rec)))

        listItem.add(createItem(getString(R.string.backup_action_title_dtbo), getString(R.string.backup_action_desc_dtbo)))
        listItem.add(createItem(getString(R.string.restore_action_title_dtbo), getString(R.string.restore_action_desc_dtbo)))

        listItem.add(createItem(getString(R.string.backup_action_title_persist), getString(R.string.backup_action_desc_persist)))
        listItem.add(createItem(getString(R.string.restore_action_title_persist), getString(R.string.restore_action_desc_persist)))

        val mSimpleAdapter = SimpleAdapter(
                this,
                listItem,
                R.layout.list_item_action,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        img_action_listview.adapter = mSimpleAdapter


        img_action_listview.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> backupImg(BOOT_IMG)
                1 -> chooseImaToFlash(BOOT_IMG)

                2 -> backupImg(RECOVERY_IMG)
                3 -> chooseImaToFlash(RECOVERY_IMG)

                4 -> backupImg(DTBO_IMG)
                5 -> chooseImaToFlash(DTBO_IMG)

                6 -> backupImg(PERSIST_IMG)
                7 -> chooseImaToFlash(PERSIST_IMG)
            }
        }
    }

    private fun backupImg(action: Int) {
        if (getSDFreeSizeMB() < 200) {
            Scene.toast(getString(R.string.backup_space_small), Toast.LENGTH_LONG)
            return
        } else {
            var fileName = ""
            when (action) {
                BOOT_IMG -> fileName = "boot"
                RECOVERY_IMG -> fileName = "recovery"
                DTBO_IMG -> fileName = "dtbo"
                PERSIST_IMG -> fileName = "persist"
            }
            if (File("${CommonCmds.SDCardDir}/$fileName.img").exists()) {
                DialogHelper.animDialog(AlertDialog.Builder(this)
                        .setTitle(getString(R.string.backup_file_exists))
                        .setMessage(String.format(getString(R.string.backup_img_exists), fileName))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            backupImgConfirm(action)
                        })
            } else {
                backupImgConfirm(action)
            }
        }
    }

    private fun backupImgConfirm(action: Int) {
        when (action) {
            BOOT_IMG -> BackupRestoreUtils(this).saveBoot()
            RECOVERY_IMG -> BackupRestoreUtils(this).saveRecovery()
            DTBO_IMG -> BackupRestoreUtils(this).saveDTBO()
            PERSIST_IMG -> BackupRestoreUtils(this).savePersist()
        }
    }

    private val BOOT_IMG = 0
    private val RECOVERY_IMG = 1
    private val DTBO_IMG = 2
    private val PERSIST_IMG = 3

    private fun chooseImaToFlash(type: Int) {
        val intent = Intent(this, ActivityFileSelector::class.java)
        intent.putExtra("extension", "img")
        startActivityForResult(intent, type)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null && data.extras?.containsKey("file") == true) {
            val path = data.extras!!.getString("file")!!
            //刷入recovery
            if (File(path).exists()) {
                var partition = ""
                when (requestCode) {
                    BOOT_IMG -> partition = "Boot"
                    RECOVERY_IMG -> partition = "Recovery"
                    DTBO_IMG -> partition = "DTBO"
                    PERSIST_IMG -> partition = "Persist"
                }
                DialogHelper.animDialog(AlertDialog.Builder(this)
                        .setTitle(getString(R.string.flash_confirm))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            when (requestCode) {
                                BOOT_IMG -> BackupRestoreUtils(this).flashBoot(path)
                                RECOVERY_IMG -> BackupRestoreUtils(this).flashRecovery(path)
                                DTBO_IMG -> BackupRestoreUtils(this).flashDTBO(path)
                                PERSIST_IMG -> BackupRestoreUtils(this).flashPersist(path)
                            }
                        }
                        .setMessage("此操作将刷入${path}到系统${partition}分区，如果你选择了错误的文件，刷入后可能导致手机无法开机！"))
            }
        } else {
            Toast.makeText(this, "所选的文件没找到！", Toast.LENGTH_SHORT).show()
        }
    }
}
