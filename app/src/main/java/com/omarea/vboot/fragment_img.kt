package com.omarea.vboot

import android.content.DialogInterface
import kotlinx.android.synthetic.main.layout_img.*
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter

import com.omarea.shared.FileSelectType
import com.omarea.shared.cmd_shellTools

import java.io.File
import java.util.ArrayList
import java.util.HashMap


class fragment_img : Fragment() {
    internal var cmdshellTools: cmd_shellTools? = null
    internal var thisview: main? = null

    fun createItem(title: String, desc: String): HashMap<String, Any> {
        val item = HashMap<String, Any>()
        item.put("Title", title)
        item.put("Desc", desc)
        return item
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.layout_img, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        val listItem = ArrayList<HashMap<String, Any>>()/*在数组中存放数据*/

        listItem.add(createItem(context.getString(R.string.backup_action_title_boot), context.getString(R.string.backup_action_desc_boot)))
        listItem.add(createItem(context.getString(R.string.restore_action_title_boot), context.getString(R.string.restore_action_desc_boot)))
        listItem.add(createItem(context.getString(R.string.backup_action_title_rec), context.getString(R.string.backup_action_desc_rec)))
        listItem.add(createItem(context.getString(R.string.restore_action_title_rec), context.getString(R.string.restore_action_desc_rec)))
        listItem.add(createItem(context.getString(R.string.zip_package), context.getString(R.string.zip_package_desc)))

        val mSimpleAdapter = SimpleAdapter(
                view!!.context, listItem,
                R.layout.action_row_item,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        img_action_listview.adapter = mSimpleAdapter


        img_action_listview.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            when (position) {
                0 -> {
                    if (cmdshellTools!!.GetSDFreeSizeMB() < 100) {
                        Snackbar.make(view, context.getString(R.string.backup_space_small), Snackbar.LENGTH_LONG).show()
                        return@OnItemClickListener
                    }
                    if (File("/sdcard/boot.img").exists()) {
                        val builder = AlertDialog.Builder(thisview!!)
                        builder.setTitle(context.getString(R.string.backup_file_exists))
                        builder.setNegativeButton(android.R.string.cancel, null)
                        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
                            //导出boot
                            cmdshellTools!!.SaveBoot("")
                        }
                        builder.setMessage(context.getString(R.string.backup_boot_exists))
                        builder.create().show()
                    } else {
                        //导出boot
                        cmdshellTools!!.SaveBoot("")
                    }
                }
                1 -> {
                    //刷入boot
                    val builder = AlertDialog.Builder(thisview!!)
                    builder.setTitle(context.getString(R.string.restore_boot_confirm))
                    builder.setNegativeButton(android.R.string.cancel, null)
                    builder.setPositiveButton(android.R.string.yes) { dialog, which ->
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = "*/img"
                        intent.addCategory(Intent.CATEGORY_OPENABLE)
                        startActivityForResult(intent, 1)
                        thisview!!.setfileSelectType(FileSelectType.BootFlash)
                    }
                    builder.setMessage(context.getString(R.string.restore_boot_confirm_desc))
                    builder.create().show()
                }
                2 -> {
                    if (cmdshellTools!!.GetSDFreeSizeMB() < 100) {
                        Snackbar.make(view, context.getString(R.string.backup_space_small), Snackbar.LENGTH_LONG).show()
                        return@OnItemClickListener
                    }
                    if (File("/sdcard/recovery.img").exists()) {
                        val builder = AlertDialog.Builder(thisview!!)
                        builder.setTitle(context.getString(R.string.backup_file_exists))
                        builder.setNegativeButton(android.R.string.cancel, null)
                        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
                            //导出rec
                            cmdshellTools!!.SaveRecovery("")
                        }
                        builder.setMessage(context.getString(R.string.backup_rec_exists))
                        builder.create().show()
                    } else {
                        //导出rec
                        cmdshellTools!!.SaveRecovery("")
                    }
                }
                3 -> {
                    //刷入rec
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "*/img"//设置MIME类型
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    startActivityForResult(intent, 1)
                    thisview!!.setfileSelectType(FileSelectType.RecFlash)
                }
                4 -> {
                    //打包rom
                    val intent = Intent(thisview, rom2zip::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    companion object {
        fun Create(thisView: main, cmdshellTools: cmd_shellTools): Fragment {
            val fragment = fragment_img()
            fragment.cmdshellTools = cmdshellTools
            fragment.thisview = thisView
            return fragment
        }
    }
}
