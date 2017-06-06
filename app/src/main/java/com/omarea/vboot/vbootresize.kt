package com.omarea.vboot

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.*
import com.omarea.shared.cmd_shellTools
import java.io.IOException

class vbootresize : AppCompatActivity() {
    lateinit internal var view: vbootresize
    lateinit internal var cmdshellTools: cmd_shellTools

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vbootresize)
        val toolbar = findViewById(R.id.toolbar1) as Toolbar
        setSupportActionBar(toolbar)

        view = this
        val progressBar = findViewById(R.id.progressBar2) as ProgressBar
        cmdshellTools = cmd_shellTools(this, progressBar)
        val resizesavedata = findViewById(R.id.resizesavedata) as CheckBox

        resizesavedata.setOnClickListener {
            if (!resizesavedata.isChecked) {
                val builder = AlertDialog.Builder(view)
                builder.setTitle("数据安全警告！")
                builder.setMessage("真的要以不保存数据方式调整系统二容量？\n这可以加快速度，但会导致系统二用户数据丢失。")
                builder.setNegativeButton(android.R.string.cancel) { dialog, which -> resizesavedata.isChecked = true }
                builder.setOnCancelListener { resizesavedata.isChecked = true }
                builder.setPositiveButton(android.R.string.yes, null)
                builder.create().show()
            }
        }

        findViewById(R.id.CommitBtn).setOnClickListener { v ->
            val text = (findViewById(R.id.DataSizeValue) as EditText).text.toString()
            if (text !== "" && text.trim { it <= ' ' } !== "") {
                val value = Integer.parseInt(text)
                if (value < 1280) {
                    Snackbar.make(v, "抱歉，至少应为系统二分配1.3G空间，否则可能无法正常启动！", Snackbar.LENGTH_SHORT).show()
                } else {
                    val builder = AlertDialog.Builder(view)
                    if (resizesavedata.isChecked) {
                        builder.setTitle("确定调整容量？")
                        builder.setMessage("为确保资料安全，建议您先备份系统二上的重要数据！\n")
                        builder.setPositiveButton(android.R.string.yes) { dialog, which -> cmdshellTools.VBOOTDataReSize(value) }
                    } else {
                        builder.setTitle("确定不保留数据？")
                        builder.setMessage("调整系统二容量且不保留数据？\n请确保您已备份系统二上的重要数据！！！\n")
                        builder.setPositiveButton(android.R.string.yes) { dialog, which -> cmdshellTools.CreateVBOOTData(value) }
                    }
                    builder.setNegativeButton(android.R.string.cancel, null)
                    builder.create().show()
                }
            }
        }
        try {
            val dataimgusesize = findViewById(R.id.dataimgusesize) as TextView
            val dataimgfreesize = findViewById(R.id.dataimgfreesize) as TextView
            val dataimgtotalsize = findViewById(R.id.dataimgtotalsize) as TextView
            val sysdatafreesize = findViewById(R.id.sysdatafreesize) as TextView


            val myHandler = object : Handler() {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                }
            }

            progressBar.visibility = View.VISIBLE
            Toast.makeText(this, "正在检查和修复系统二Data，请稍等！", Toast.LENGTH_SHORT).show()
            Thread(Runnable {
                try {
                    val useSize = cmdshellTools.GetImgUseDataSizeMB()
                    val freeSize = cmdshellTools.GetImgFreeSizeMB()
                    val totalSize = cmdshellTools.GetVBOOTDataSize()
                    val sdcardSize = cmdshellTools.GetSDFreeSizeMB()
                    myHandler.post {
                        dataimgusesize.text = "Data.img已用：" + useSize + "MB"
                        dataimgfreesize.text = "Data.img可用：" + freeSize + "MB"
                        dataimgtotalsize.text = "Data.img总计：" + totalSize + "MB"
                        sysdatafreesize.text = "SD Card  可用：" + sdcardSize + "MB"
                        progressBar.visibility = View.GONE
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }).start()

            //dataimgusesize.setText("Data.img已用："+cmdshellTools.GetImgUseDataSizeMB()+"MB");
            //dataimgfreesize.setText("Data.img可用："+cmdshellTools.GetImgFreeSizeMB()+"MB");
            //dataimgtotalsize.setText("Data.img总计："+cmdshellTools.GetVBOOTDataSize()+"MB");
            //sysdatafreesize.setText("SD Card  可用："+cmdshellTools.GetSDFreeSizeMB()+"MB");
        } catch (ex: Exception) {

        }

    }


}