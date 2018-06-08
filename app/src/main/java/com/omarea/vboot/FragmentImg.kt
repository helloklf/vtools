package com.omarea.vboot

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
import com.omarea.shared.Consts
import com.omarea.shell.units.BackupRestoreUnit
import kotlinx.android.synthetic.main.layout_img.*
import java.io.File
import java.util.*


class FragmentImg : Fragment() {
    internal var thisview: ActivityMain? = null

    fun createItem(title: String, desc: String): HashMap<String, Any> {
        val item = HashMap<String, Any>()
        item.put("Title", title)
        item.put("Desc", desc)
        return item
    }

    //获取SD卡可用空间
    fun GetSDFreeSizeMB(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.availableBytes / 1024 / 1024 //剩余空间
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_img, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val listItem = ArrayList<HashMap<String, Any>>()/*在数组中存放数据*/

        listItem.add(createItem(context!!.getString(R.string.backup_action_title_boot), context!!.getString(R.string.backup_action_desc_boot)))
        listItem.add(createItem(context!!.getString(R.string.restore_action_title_boot), context!!.getString(R.string.restore_action_desc_boot)))
        listItem.add(createItem(context!!.getString(R.string.backup_action_title_rec), context!!.getString(R.string.backup_action_desc_rec)))
        listItem.add(createItem(context!!.getString(R.string.restore_action_title_rec), context!!.getString(R.string.restore_action_desc_rec)))
        //listItem.add(createItem(context.getString(R.string.restore_action_title_rec), context.getString(R.string.restore_action_desc_rec)))

        val mSimpleAdapter = SimpleAdapter(
                view.context, listItem,
                R.layout.action_row_item,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        img_action_listview.adapter = mSimpleAdapter


        img_action_listview.onItemClickListener = AdapterView.OnItemClickListener { _, view, position, _ ->
            when (position) {
                0 -> {
                    if (GetSDFreeSizeMB() < 100) {
                        Snackbar.make(view, context!!.getString(R.string.backup_space_small), Snackbar.LENGTH_LONG).show()
                        return@OnItemClickListener
                    }
                    if (File("${Consts.SDCardDir}/boot.img").exists()) {
                        val builder = AlertDialog.Builder(thisview!!)
                        builder.setTitle(context!!.getString(R.string.backup_file_exists))
                        builder.setNegativeButton(android.R.string.cancel, null)
                        builder.setPositiveButton(android.R.string.yes) { _, _ ->
                            //导出boot
                            BackupRestoreUnit(activity!!).SaveBoot()
                        }
                        builder.setMessage(context!!.getString(R.string.backup_boot_exists))
                        builder.create().show()
                    } else {
                        //导出boot
                        BackupRestoreUnit(activity!!).SaveBoot()
                    }
                }
                1 -> {
                    //刷入boot
                    if (File("${Consts.SDCardDir}/boot.img").exists()) {
                        val builder = AlertDialog.Builder(thisview!!)
                        builder.setTitle("确定刷入${Consts.SDCardDir}/boot.img？")
                        builder.setNegativeButton(android.R.string.cancel, null)
                        builder.setPositiveButton(android.R.string.yes) { _, _ ->
                            BackupRestoreUnit(activity!!).FlashBoot("${Consts.SDCardDir}/boot.img")
                        }
                        builder.setMessage("此操作将刷入${Consts.SDCardDir}/boot.img到系统Boot分区，我十分不推荐你这么做，刷入无效的Boot文件可能导致你的设备无法启动。如果你没有办法在设备无法启动时紧急恢复。")
                        builder.create().show()
                    } else {
                        val builder = AlertDialog.Builder(thisview!!)
                        builder.setTitle("")
                        builder.setNegativeButton("", null)
                        builder.setPositiveButton(android.R.string.yes) { _, _ ->
                        }
                        builder.setMessage("由于安卓系统的文件选择器兼容性差异，现在做文件选择变得非常困难，因此不再支持自选文件刷入。请将你要刷入的Boot文件放到以下位置：\n" +
                                "${Consts.SDCardDir}/boot.img\n" +
                                "路径和文件名区分大小写")
                        builder.create().show()
                    }
                }
                2 -> {
                    if (GetSDFreeSizeMB() < 100) {
                        Snackbar.make(view, context!!.getString(R.string.backup_space_small), Snackbar.LENGTH_LONG).show()
                        return@OnItemClickListener
                    }
                    if (File("${Consts.SDCardDir}/recovery.img").exists()) {
                        val builder = AlertDialog.Builder(thisview!!)
                        builder.setTitle(context!!.getString(R.string.backup_file_exists))
                        builder.setNegativeButton(android.R.string.cancel, null)
                        builder.setPositiveButton(android.R.string.yes) { _, _ ->
                            //导出rec
                            BackupRestoreUnit(context!!).SaveRecovery()
                        }
                        builder.setMessage(context!!.getString(R.string.backup_rec_exists))
                        builder.create().show()
                    } else {
                        //导出rec
                        BackupRestoreUnit(context!!).SaveRecovery()
                    }
                }
                3 -> {
                    //刷入recovery
                    if (File("${Consts.SDCardDir}/recovery.img").exists()) {
                        val builder = AlertDialog.Builder(thisview!!)
                        builder.setTitle("确认刷入${Consts.SDCardDir}/recovery.img？")
                        builder.setNegativeButton(android.R.string.cancel, null)
                        builder.setPositiveButton(android.R.string.yes) { _, _ ->
                            BackupRestoreUnit(context!!).FlashRecovery("${Consts.SDCardDir}/recovery.img")
                        }
                        builder.setMessage("此操作将刷入${Consts.SDCardDir}/reovery.img到系统Recovery分区，应用无法验证该文件是否有效，你需要自己确保该recovery镜像适合本设备使用！")
                        builder.create().show()
                    } else {
                        val builder = AlertDialog.Builder(thisview!!)
                        builder.setTitle("")
                        builder.setNegativeButton("", null)
                        builder.setPositiveButton(android.R.string.yes) { _, _ ->
                        }
                        builder.setMessage("由于安卓系统的文件选择器兼容性差异，现在做文件选择变得非常困难，因此不再支持自选文件刷入。请将你要刷入的recovery文件放到以下位置：\n" +
                                "${Consts.SDCardDir}/recovery.img\n" +
                                "路径和文件名区分大小写")
                        builder.create().show()
                    }

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

    companion object {
        fun createPage(thisView: ActivityMain): Fragment {
            val fragment = FragmentImg()
            fragment.thisview = thisView
            return fragment
        }
    }
}
