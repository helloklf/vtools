package com.omarea.vtools.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.omarea.shared.CommonCmds
import com.omarea.shared.FileWrite
import com.omarea.shared.MagiskExtend
import com.omarea.shell.RootFile
import com.omarea.ui.AdapterFileSelector
import com.omarea.ui.ProgressBarDialog
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.fragment_magisk.*
import java.io.File


class FragmentMagisk : Fragment() {
    private var adapterFileSelector: AdapterFileSelector? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_magisk, container, false)

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (MagiskExtend.magiskSupported()) {
            if (!MagiskExtend.moduleInstalled()) {
                AlertDialog.Builder(context!!).setTitle("安装Magisk拓展？")
                        .setMessage("安装Scene提供的Magisk拓展模块，从而在不修改系统文件的情况下，更改一些参数~")
                        .setPositiveButton(R.string.btn_confirm, {
                            _, _ ->
                            MagiskExtend.magiskModuleInstall(this.context)
                            Toast.makeText(context, "操作已执行~", Toast.LENGTH_LONG).show()
                            this@FragmentMagisk.activity!!.recreate()
                        })
                        .setNegativeButton(R.string.btn_cancel, {
                            _, _ ->
                        })
                        .create()
                        .show()
            }
        } else {
            Toast.makeText(context, "您的设备未安装Magisk框架，不能使用本功能~", Toast.LENGTH_LONG).show()
            return
        }

        magisk_tabhost.setup()

        magisk_tabhost.addTab(magisk_tabhost.newTabSpec("system.prop").setContent(R.id.magisk_tab1).setIndicator("system.prop"))
        magisk_tabhost.addTab(magisk_tabhost.newTabSpec("system.file").setContent(R.id.magisk_tab2).setIndicator("system"))
        magisk_tabhost.addTab(magisk_tabhost.newTabSpec("before_start").setContent(R.id.magisk_tab3).setIndicator("post-fs-data"))
        magisk_tabhost.addTab(magisk_tabhost.newTabSpec("after_start").setContent(R.id.magisk_tab4).setIndicator("service"))
        magisk_tabhost.currentTab = 0

        magisk_props.setText(MagiskExtend.getProps());
        magisk_props_reset.setOnClickListener {
            magisk_props.setText(MagiskExtend.getProps());
        }
        magisk_props_save.setOnClickListener {
            if (FileWrite.writePrivateFile(magisk_props.text.toString().toByteArray(), "magisk_system.prop", context!!)) {
                if (MagiskExtend.updateProps(FileWrite.getPrivateFilePath(context!!, "magisk_system.prop"))) {
                    magisk_props.setText(MagiskExtend.getProps());
                    Toast.makeText(context!!, "已保存更改，重启后生效 ^_~ ", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Magisk镜像空间不足，操作失败！~", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context!!, "保存失败!_*", Toast.LENGTH_LONG).show()
            }
        }


        magisk_beforestart.setText(MagiskExtend.getFsPostDataSH());
        magisk_beforestart_reset.setOnClickListener {
            magisk_beforestart.setText(MagiskExtend.getFsPostDataSH());
        }
        magisk_beforestart_save.setOnClickListener {
            if (FileWrite.writePrivateFile(magisk_beforestart.text.toString().toByteArray(), "magisk_post-fs-data.sh", context!!)) {
                if(MagiskExtend.updateFsPostDataSH(FileWrite.getPrivateFilePath(context!!, "magisk_post-fs-data.sh"))){
                    magisk_beforestart.setText(MagiskExtend.getFsPostDataSH());
                    Toast.makeText(context!!, "已保存更改，重启后生效 ^_~ ", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Magisk镜像空间不足，操作失败！~", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context!!, "保存失败!_*", Toast.LENGTH_LONG).show()
            }
        }


        magisk_afterstart.setText(MagiskExtend.getServiceSH());
        magisk_afterstart_reset.setOnClickListener {
            magisk_afterstart.setText(MagiskExtend.getServiceSH());
        }
        magisk_afterstart_save.setOnClickListener {
            if (FileWrite.writePrivateFile(magisk_afterstart.text.toString().toByteArray(), "magisk_service.sh", context!!)) {
                if (MagiskExtend.updateServiceSH(FileWrite.getPrivateFilePath(context!!, "magisk_service.sh"))) {
                    magisk_afterstart.setText(MagiskExtend.getServiceSH());
                    Toast.makeText(context!!, "已保存更改，重启后生效 ^_~ ", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Magisk镜像空间不足，操作失败！~", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context!!, "保存失败!_*", Toast.LENGTH_LONG).show()
            }
        }

        val sdcard = File(CommonCmds.SDCardDir)
        if (sdcard.exists() && sdcard.isDirectory) {
            val list = sdcard.listFiles()
            if (list == null) {
                Toast.makeText(context, "没有读取文件的权限！", Toast.LENGTH_LONG).show()
                return
            }
            adapterFileSelector = AdapterFileSelector(File(MagiskExtend.MAGISK_PATH + "system"), Runnable {
                val file: File? = adapterFileSelector!!.selectedFile
                if (file != null) {
                }
            }, ProgressBarDialog(this.context!!), null, false, true, Runnable {
                val file: File? = adapterFileSelector!!.selectedFile
                if (file != null) {
                    RootFile.deleteDirOrFile(file.absolutePath);
                    adapterFileSelector!!.refresh();
                }
            }, false)
            magisk_files.adapter = adapterFileSelector
        } else {

        }
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentMagisk()
            return fragment
        }
    }
}