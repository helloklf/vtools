package com.omarea.vboot.dialogs

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.omarea.shared.FileWrite
import com.omarea.shared.Consts
import com.omarea.shell.AsynSuShellUnit
import com.omarea.shell.Files
import com.omarea.vboot.R


/**
 * Created by Hello on 2017/12/20.
 */

class DialogFilesHardLinks2(private var context: Context) {
    private var handler: Handler

    //检查文件列表
    fun checkFileList() {
        getApkFiles()
    }

    fun getApkFiles() {
        FileWrite.WritePrivateFile(context.assets, "addin/map-dualboot-apk.sh", "map-dualboot-apk.sh", context)
        val shellFile = "${FileWrite.getPrivateFileDir(context)}/map-dualboot-apk.sh"
        val sb = StringBuilder()
        sb.append("cp $shellFile /cache/map-dualboot-apk.sh;")
        sb.append("chmod 777 /cache/map-dualboot-apk.sh;")
        sb.append("sh /cache/map-dualboot-apk.sh;")
        sb.append("echo '[operation completed]';")
        execShell(sb)
    }


    private fun execShell(sb: StringBuilder) {
        val layoutInflater = LayoutInflater.from(context)
        val dialog = layoutInflater.inflate(R.layout.dialog_app_options, null)
        val textView = (dialog.findViewById(R.id.dialog_app_details_pkgname) as TextView)
        textView.minLines = 4
        textView.maxLines = 4
        textView.text = "正在获取权限"
        val alert = AlertDialog.Builder(context).setView(dialog).setCancelable(false).create()
        AsynSuShellUnit(ProgressHandler(dialog, alert, handler)).exec(sb.toString()).waitFor()
        alert.show()
    }

    class ProgressHandler(dialog: View, private var alert: AlertDialog, private var handler: Handler) : Handler() {
        private var textView: TextView = (dialog.findViewById(R.id.dialog_app_details_pkgname) as TextView)
        var progressBar: ProgressBar = (dialog.findViewById(R.id.dialog_app_details_progress) as ProgressBar)
        var fileInfos: ArrayList<String> = ArrayList()

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.obj != null) {
                if (msg.what == 0) {
                    textView.text = "即将开始分析所有安装的应用，期间请不要安装或更新应用!!!"
                } else {
                    val obj = msg.obj.toString()
                    if (obj == "[operation completed]") {
                        progressBar.progress = 100
                        textView.text = "操作完成！"
                        handler.postDelayed({
                            alert.dismiss()
                            alert.hide()
                        }, 2000)
                        handler.handleMessage(handler.obtainMessage(2, fileInfos))
                    } else if (Regex("^\\[.*\\]\$").matches(obj)) {
                        if (Regex("^\\[fileinfo.*\\]$").matches(obj)) {
                            fileInfos.add(obj)
                        } else {
                            progressBar.progress = msg.what
                            val txt = obj
                                    .replace("[mapfiles", "[遍历文件")
                                    .replace("[totalfile", "[文件总数")
                                    .replace("[compute-md5", "[计算MD5")
                                    .replace("[fileinfo", "[文件信息")
                            textView.text = txt
                        }
                    }
                }
            }
        }

        init {
            textView.text = "正在获取权限"
        }
    }

    class AppInfosGetedHandler(private var context: Context,private var next:Runnable): Handler(){
        class DiskFileInfo {
            var md5:String = "";
            var inode:String = "";
            var path:String = "";
            var sizeKB:Int = 0;
        }
        private fun splitAppinfos(appinfos:ArrayList<String>?): HashMap<String, HashMap<String, ArrayList<DiskFileInfo>>>? {
            if (appinfos == null || appinfos.size == 0)
                return null
            //<MD5,<INode,Path[]>>
            val md5Map = HashMap<String, HashMap<String, ArrayList<DiskFileInfo>>>()
            for (appinfo in appinfos) {
                val row = appinfo.substring("[fileinfo:".length, appinfo.length -1).split(";")
                val inode = row[0]
                val md5 = row[1]
                val path = row[2]
                val size = row[3].toInt()

                val diskFile = DiskFileInfo()
                diskFile.inode = inode
                diskFile.md5 = md5
                diskFile.path = path
                diskFile.sizeKB = size

                if (md5Map.containsKey(md5)) {
                    val inodeMap = md5Map[md5]
                    if (inodeMap!!.containsKey(inode)) {
                        inodeMap[inode]!!.add(diskFile)
                    } else {
                        inodeMap.put(inode, arrayListOf(diskFile))
                    }
                } else {
                    md5Map.put(md5, hashMapOf(Pair(inode, arrayListOf(diskFile))))
                }
            }

            return md5Map
        }

        private fun getRedundancy(md5Map:HashMap<String, HashMap<String, ArrayList<DiskFileInfo>>>?){
            if (md5Map == null)
                return

            var redundancy = 0
            var mbSize = 0
            for (map in md5Map) {
                val values = map.value
                if (values.size > 0) {
                    mbSize += ((values.size - 1) * (values.get(values.keys.first())!![0].sizeKB))
                    redundancy += (values.size - 1)
                }
            }
            if (redundancy == 0) {
                AlertDialog.Builder(context).setTitle("Emmm，看来...").setMessage("现在已经是最佳状态，没有可回收的空间！").create().show()
                return
            }
            AlertDialog.Builder(context)
                    .setTitle("文件检索完成")
                    .setMessage("找到${redundancy}个冗余文件，处理以后可节省${mbSize/1024}MB存储空间\n\n但该功能是试验性的，可能会导致应用闪退、应用丢失甚至需要恢复出厂设置！！！")
                    .setNegativeButton("处理", {
                        _,_ ->
                        hardlinkMerge(md5Map)
                        next.run()
                    })
                    .setNeutralButton("取消", {
                        _,_ ->
                    })
                    .create()
                    .show()
        }

        class HardlinkMergeHandler(private var runnable: Runnable, dialog: AlertDialog): Handler(){
            private var textView: TextView = (dialog.findViewById(R.id.dialog_app_details_pkgname) as TextView)

            override fun handleMessage(msg: Message?) {
                super.handleMessage(msg)
                if (msg == null)
                    return

                if (msg.what == 1) {
                    val obj = msg.obj.toString()
                    if (obj == "[operation completed]") {
                        textView.setText("操作完成！")
                        this.postDelayed(runnable, 2000)
                    } else if (Regex("^\\[.*\\]\$").matches(obj)) {
                        val txt = obj
                                .replace("[linked", "[已链接 ")
                        textView.text = txt
                    }
                }
            }
        }

        private fun buildScript(md5map:HashMap<String, HashMap<String, ArrayList<DiskFileInfo>>>): StringBuilder {
            val stringBuilder = StringBuilder()
            for (group in md5map) {
                //primary file
                val primary = group.value[group.value.keys.first()]!![0].path
                stringBuilder.append("chmod 777 ${primary};")
                for (inodeGroup in group.value) {
                    for (file in inodeGroup.value) {
                        if (file.path != primary) {
                            stringBuilder.append("ln -f \"$primary\" \"${file.path}\";")
                            stringBuilder.append("echo [linked ${file.path}];")
                        }
                    }
                }
            }
            stringBuilder.append("echo [operation completed];")
            return stringBuilder
        }

        private fun execShell(sb: StringBuilder) {
            val layoutInflater = LayoutInflater.from(context)
            val dialog = layoutInflater.inflate(R.layout.dialog_app_options, null)
            val textView = (dialog.findViewById(R.id.dialog_app_details_pkgname) as TextView)
            textView.minLines = 4
            textView.maxLines = 4
            textView.text = "开始链接相同文件"
            val alert = AlertDialog.Builder(context).setView(dialog).setCancelable(false).create()
            alert.show()
            val size = Files.GetDirFreeSizeMB(Consts.SDCardDir)
            AsynSuShellUnit(HardlinkMergeHandler(Runnable {
                alert.cancel()
                alert.hide()
                val currentSize = Files.GetDirFreeSizeMB(Consts.SDCardDir)
                AlertDialog.Builder(context).setTitle("全部完成").setMessage("大约节省了${currentSize - size}MB空间").create().show()
            }, alert)).exec(sb.toString()).waitFor()
        }

        private fun hardlinkMerge(md5map:HashMap<String, HashMap<String, ArrayList<DiskFileInfo>>>) {
            execShell(buildScript(md5map))
        }

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)

            if (msg == null)
                return

            if (msg.what == 2) {
                if (msg.obj == null)
                    return
                getRedundancy(splitAppinfos(msg.obj as ArrayList<String>?))
            }
        }
    }

    init {
        handler = AppInfosGetedHandler(context, Runnable {
        })
    }
}
