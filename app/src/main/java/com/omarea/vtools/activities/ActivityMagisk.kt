package com.omarea.vtools.activities

import android.os.Bundle
import android.widget.Toast
import com.omarea.common.shared.FileWrite
import com.omarea.common.shared.MagiskExtend
import com.omarea.common.shared.RootFileInfo
import com.omarea.common.shell.RootFile
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.ui.AdapterRootFileSelector
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_magisk.*
import java.io.File


class ActivityMagisk : ActivityBase() {
    private var adapterFileSelector: AdapterRootFileSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_magisk)

        setBackArrow()

        onViewCreated()
    }

    override fun onResume() {
        super.onResume()
        title = getString(R.string.menu_app_magisk)
    }

    fun onViewCreated() {
        if (MagiskExtend.magiskSupported()) {
            if (!MagiskExtend.moduleInstalled()) {
                DialogHelper.confirm(this, "安装Magisk拓展？",
                        "安装Scene提供的Magisk拓展模块，从而在不修改系统文件的情况下，更改一些参数~",
                        {
                            MagiskExtend.magiskModuleInstall(context)
                            Toast.makeText(context, "操作已执行~", Toast.LENGTH_LONG).show()
                            this@ActivityMagisk.recreate()
                        })
            }
        } else {
            Toast.makeText(context, "您的设备未安装Magisk框架，不能使用本功能~", Toast.LENGTH_LONG).show()
            return
        }

        magisk_tabhost.setup()

        magisk_tabhost.addTab(magisk_tabhost.newTabSpec("system.prop").setContent(R.id.magisk_tab1).setIndicator("属性"))
        magisk_tabhost.addTab(magisk_tabhost.newTabSpec("system.file").setContent(R.id.magisk_tab2).setIndicator("系统文件"))
        magisk_tabhost.addTab(magisk_tabhost.newTabSpec("before_start").setContent(R.id.magisk_tab3).setIndicator("启动前"))
        magisk_tabhost.addTab(magisk_tabhost.newTabSpec("after_start").setContent(R.id.magisk_tab4).setIndicator("启动后"))
        magisk_tabhost.currentTab = 0

        magisk_props.setText(MagiskExtend.getProps())
        magisk_props_reset.setOnClickListener {
            magisk_props.setText(MagiskExtend.getProps())
        }
        magisk_props_save.setOnClickListener {
            if (FileWrite.writePrivateFile((magisk_props.text.toString() + "\n").toByteArray(), "magisk_system.prop", context)) {
                val file = FileWrite.getPrivateFilePath(context, "magisk_system.prop")
                if (MagiskExtend.updateProps(file)) {
                    magisk_props.setText(MagiskExtend.getProps())
                    Toast.makeText(context, "已保存更改，重启后生效 ^_~ ", Toast.LENGTH_LONG).show()
                    File(file).delete()
                } else {
                    Toast.makeText(context, "Magisk镜像空间不足，操作失败！~", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "保存失败!_*", Toast.LENGTH_LONG).show()
            }
        }


        magisk_beforestart.setText(MagiskExtend.getFsPostDataSH())
        magisk_beforestart_reset.setOnClickListener {
            magisk_beforestart.setText(MagiskExtend.getFsPostDataSH())
        }
        magisk_beforestart_save.setOnClickListener {
            if (FileWrite.writePrivateFile((magisk_beforestart.text.toString() + "\n").toByteArray(), "magisk_post-fs-data.sh", context)) {
                val file = FileWrite.getPrivateFilePath(context, "magisk_post-fs-data.sh")
                if (MagiskExtend.updateFsPostDataSH(file)) {
                    magisk_beforestart.setText(MagiskExtend.getFsPostDataSH())
                    Toast.makeText(context, "已保存更改，重启后生效 ^_~ ", Toast.LENGTH_LONG).show()
                    File(file).delete()
                } else {
                    Toast.makeText(context, "Magisk镜像空间不足，操作失败！~", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "保存失败!_*", Toast.LENGTH_LONG).show()
            }
        }


        magisk_afterstart.setText(MagiskExtend.getServiceSH())
        magisk_afterstart_reset.setOnClickListener {
            magisk_afterstart.setText(MagiskExtend.getServiceSH())
        }
        magisk_afterstart_save.setOnClickListener {
            if (FileWrite.writePrivateFile((magisk_afterstart.text.toString() + "\n").toByteArray(), "magisk_service.sh", context)) {
                val file = FileWrite.getPrivateFilePath(context, "magisk_service.sh")
                if (MagiskExtend.updateServiceSH(file)) {
                    magisk_afterstart.setText(MagiskExtend.getServiceSH())
                    Toast.makeText(context, "已保存更改，重启后生效 ^_~ ", Toast.LENGTH_LONG).show()
                    File(file).delete()
                } else {
                    Toast.makeText(context, "Magisk镜像空间不足，操作失败！~", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "保存失败!_*", Toast.LENGTH_LONG).show()
            }
        }
        adapterFileSelector = AdapterRootFileSelector(RootFileInfo(MagiskExtend.MAGISK_PATH + "system"), {
            val file: RootFileInfo? = adapterFileSelector!!.selectedFile
        }, ProgressBarDialog(this), null, false, true, {
            val file: RootFileInfo? = adapterFileSelector!!.selectedFile
            if (file != null) {
                RootFile.deleteDirOrFile(file.absolutePath)
                adapterFileSelector!!.refresh()
            }
        }, false)
        magisk_files.adapter = adapterFileSelector
    }
}