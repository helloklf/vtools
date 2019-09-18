package com.omarea.vtools.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.SimpleAdapter
import android.widget.Toast
import com.omarea.shell_utils.BackupRestoreUtils
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R
import com.omarea.vtools.activities.ActivityFileSelector
import kotlinx.android.synthetic.main.fragment_img.*
import java.io.File
import java.util.*


class FragmentImg : Fragment() {
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_img, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val listItem = ArrayList<HashMap<String, Any>>()/*在数组中存放数据*/

        listItem.add(createItem(context!!.getString(R.string.backup_action_title_boot), context!!.getString(R.string.backup_action_desc_boot)))
        listItem.add(createItem(context!!.getString(R.string.restore_action_title_boot), context!!.getString(R.string.restore_action_desc_boot)))
        listItem.add(createItem(context!!.getString(R.string.backup_action_title_rec), context!!.getString(R.string.backup_action_desc_rec)))
        listItem.add(createItem(context!!.getString(R.string.restore_action_title_rec), context!!.getString(R.string.restore_action_desc_rec)))
        //listItem.add(createItem(context.getString(R.string.restore_action_title_rec), context.getString(R.string.restore_action_desc_rec)))

        val mSimpleAdapter = SimpleAdapter(
                view.context, listItem,
                R.layout.list_item_action,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        img_action_listview.adapter = mSimpleAdapter


        img_action_listview.onItemClickListener = AdapterView.OnItemClickListener { _, view, position, _ ->
            when (position) {
                0 -> {
                    if (getSDFreeSizeMB() < 100) {
                        Snackbar.make(view, context!!.getString(R.string.backup_space_small), Snackbar.LENGTH_LONG).show()
                        return@OnItemClickListener
                    }
                    if (File("${CommonCmds.SDCardDir}/boot.img").exists()) {
                        val builder = AlertDialog.Builder(context!!)
                        builder.setTitle(context!!.getString(R.string.backup_file_exists))
                        builder.setNegativeButton(android.R.string.cancel, null)
                        builder.setPositiveButton(android.R.string.yes) { _, _ ->
                            //导出boot
                            BackupRestoreUtils(activity!!).saveBoot()
                        }
                        builder.setMessage(context!!.getString(R.string.backup_boot_exists))
                        val dialog = builder.create()
                        dialog.window!!.setWindowAnimations(R.style.windowAnim)
                        dialog.show()
                    } else {
                        //导出boot
                        BackupRestoreUtils(activity!!).saveBoot()
                    }
                }
                1 -> {
                    val intent = Intent(this.context, ActivityFileSelector::class.java)
                    intent.putExtra("extension", "img")
                    startActivityForResult(intent, BOOT_IMG_SELECTOR)
                }
                2 -> {
                    if (getSDFreeSizeMB() < 100) {
                        Snackbar.make(view, context!!.getString(R.string.backup_space_small), Snackbar.LENGTH_LONG).show()
                        return@OnItemClickListener
                    }
                    if (File("${CommonCmds.SDCardDir}/recovery.img").exists()) {
                        val builder = AlertDialog.Builder(context!!)
                        builder.setTitle(context!!.getString(R.string.backup_file_exists))
                        builder.setNegativeButton(android.R.string.cancel, null)
                        builder.setPositiveButton(android.R.string.yes) { _, _ ->
                            //导出rec
                            BackupRestoreUtils(context!!).saveRecovery()
                        }
                        builder.setMessage(context!!.getString(R.string.backup_rec_exists))
                        val dialog = builder.create()
                        dialog.window!!.setWindowAnimations(R.style.windowAnim)
                        dialog.show()
                    } else {
                        //导出rec
                        BackupRestoreUtils(context!!).saveRecovery()
                    }
                }
                3 -> {
                    val intent = Intent(this.context, ActivityFileSelector::class.java)
                    intent.putExtra("extension", "img")
                    startActivityForResult(intent, RECOVERY_IMG_SELECTOR)

                    //刷入rec
                    //val intent = Intent(Intent.ACTION_GET_CONTENT)
                    //intent.type = "*/img"//设置MIME类型
                    //intent.type = "*/*"//设置MIME类型
                    //intent.addCategory(Intent.CATEGORY_OPENABLE)
                    //startActivityForResult(intent, 1)
                    //thisview!!.setfileSelectType(FileSelectType.RecFlash)
                }
            }
        }
    }

    val BOOT_IMG_SELECTOR = 0
    val RECOVERY_IMG_SELECTOR = 1

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RECOVERY_IMG_SELECTOR) {
            if (resultCode == Activity.RESULT_OK && data != null && data.extras.containsKey("file")) {
                val path = data.extras.getString("file")
                //刷入recovery
                if (File(path).exists()) {
                    val dialog = AlertDialog.Builder(context!!)
                            .setTitle(getString(R.string.flash_confirm))
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.yes) { _, _ ->
                                BackupRestoreUtils(context!!).flashRecovery(path)
                            }
                            .setMessage("此操作将刷入${path}到系统Recovery分区，应用无法验证该文件是否有效，你需要自己确保该recovery镜像适合本设备使用！")
                            .create()
                    dialog.window!!.setWindowAnimations(R.style.windowAnim)
                    dialog.show()
                } else {
                    Toast.makeText(context!!, "所选的文件没找到！", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (requestCode == BOOT_IMG_SELECTOR) {
            if (resultCode == Activity.RESULT_OK && data != null && data.extras.containsKey("file")) {
                val path = data.extras.getString("file")
                //刷入recovery
                if (File(path).exists()) {
                    val dialog = AlertDialog.Builder(context!!)
                            .setTitle(getString(R.string.flash_confirm))
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.yes) { _, _ ->
                                BackupRestoreUtils(activity!!).flashBoot(path)
                            }
                            .setMessage("此操作将刷入${path}到系统Boot分区，我十分不推荐你这么做，刷入无效的Boot文件可能导致你的设备无法启动。如果你没有办法在设备无法启动时紧急恢复。")
                            .create()
                    dialog.window!!.setWindowAnimations(R.style.windowAnim)
                    dialog.show()
                }
            }
        }
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentImg()
            return fragment
        }
    }
}
