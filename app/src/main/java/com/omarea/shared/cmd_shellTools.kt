package com.omarea.shared

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.os.StatFs
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ProgressBar
import java.io.DataOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder


/**
 * Created by helloklf on 2016/8/4.
 */
class cmd_shellTools(var context: Context?, var progressBar: ProgressBar?) {
    internal var myHandler: Handler = Handler()

    //显示进度条
    fun ShowProgressBar() {
        myHandler.post {
            if (progressBar != null)
                progressBar!!.visibility = View.VISIBLE
        }
    }

    //隐藏进度条
    fun HideProgressBar() {
        myHandler.post {
            if (progressBar != null)
                progressBar!!.visibility = View.GONE
        }
    }

    //显示弹窗提示
    fun ShowDialogMsg(title: String, msg: String) {
        if (context != null)
            myHandler.post {
                val builder = AlertDialog.Builder(context!!)
                builder.setTitle(title)
                builder.setPositiveButton(android.R.string.yes, null)
                builder.setMessage(msg + "\n")
                builder.create().show()
            }
    }

    internal fun NoRoot() {
        ShowDialogMsg("请检查ROOT权限", "请检查是否已ROOT手机，并允许本应用访问ROOT权限！")
    }

    //获取SD卡可用空间
    fun GetSDFreeSizeMB(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.availableBytes / 1024 / 1024 //剩余空间
    }

    //打包当前系统为Zip
    @Throws(UnsupportedEncodingException::class)
    fun Rom2Zip(boot: Boolean, sys: Boolean, rec: Boolean, other: Boolean, romName: String) {
        Rom2ZipThread(boot, rec, sys, other, URLEncoder.encode(romName, "UTF-8")).start()
    }


    internal inner class Rom2ZipThread(var has_boot: Boolean, var has_rec: Boolean, var has_sys: Boolean, var has_other: Boolean, name: String) : Thread() {

        var fileName = "newrom"

        init {
            this.fileName = if (name == "") "newrom" else name
        }

        override fun run() {
            var process: Process?
            try {
                ShowProgressBar()
                ShowDialogMsg("这需要好些时间", "这个过程需要10-20分钟（根据您的设备性能），您可以后台本应用去做别的事情。\n给手机降降温，或许速度会更快！")
                process = Runtime.getRuntime().exec("su")
                val out = DataOutputStream(process!!.outputStream)

                out.writeBytes("busybox --install /system/xbin\n")
                //out.writeBytes("busybox --install /system/bin\n");
                val cmd = "if [ ! -f \"/system/xbin/zip\" ]; then cp " + AppShared.baseUrl + "zip /system/xbin/zip; chmod 0755 /system/xbin/zip ;fi;"

                out.writeBytes(cmd)
                out.writeBytes("\n")
                out.writeBytes("rm -rf ${Consts.SDCardDir}/VBOOTROMCREATE\n")
                out.writeBytes("mkdir -p ${Consts.SDCardDir}/VBOOTROMCREATE\n")
                out.writeBytes("mkdir -p ${Consts.SDCardDir}/VBOOTROMCREATE/TMP\n")

                out.writeBytes("cp " + AppShared.baseUrl + "rom.zip ${Consts.SDCardDir}/VBOOTROMCREATE/newrom.zip\n")

                //boot
                if (has_boot)
                    out.writeBytes("dd if=/dev/block/bootdevice/by-name/boot of=${Consts.SDCardDir}/VBOOTROMCREATE/TMP/boot.img\n")
                //rec
                if (has_rec)
                    out.writeBytes("dd if=/dev/block/bootdevice/by-name/recovery of=${Consts.SDCardDir}/VBOOTROMCREATE/TMP/recovery.img\n")

                //system
                if (has_sys) {
                    out.writeBytes("dd if=/dev/block/bootdevice/by-name/system of=${Consts.SDCardDir}/VBOOTROMCREATE/TMP/system.img\n")
                }

                //other
                if (has_other) {

                }

                //zip
                out.writeBytes("cd ${Consts.SDCardDir}/VBOOTROMCREATE/TMP\n")
                out.writeBytes("zip -1 -vD -r ${Consts.SDCardDir}/VBOOTROMCREATE/newrom.zip .\n")

                val outName = "${Consts.SDCardDir}/$fileName.zip"
                //clear
                out.writeBytes("mv ${Consts.SDCardDir}/VBOOTROMCREATE/newrom.zip " + outName + "\n")
                out.writeBytes("cd /system/bin\n")
                out.writeBytes("rm -rf ${Consts.SDCardDir}/VBOOTROMCREATE\n")

                out.writeBytes("\n")

                out.writeBytes("exit")
                out.writeBytes("\n")
                out.flush()

                if (process.waitFor() == 0) {
                    ShowDialogMsg("打包成功！", "rom已复制到SD卡根目录下！\n 路径：$outName")
                } else {
                    ShowDialogMsg("打包失败！", "请检查空间是否足够。或向开发者反馈此问题！")
                }
            } catch (e: IOException) {
                e.printStackTrace()
                NoRoot()
            } catch (e: InterruptedException) {
                ShowDialogMsg("打包失败！", "请检查空间是否足够。或向开发者反馈此问题！")
                e.printStackTrace()
            } finally {
                HideProgressBar()
            }
        }
    }

    //pigz
    //tar
    //zip
    //gzip
    //bzip2
    //gunzip
    //gunzip -c /data/media/rom2/system.img.gz | dd of=/data/media/rom2/system.img
    //dd if=/data/media/rom2/system.img | gzip > /data/media/rom2/system.img.gz
    //dd if=/data/media/rom2/system.img | zip > /data/media/rom2/system.img.zip
    //在系统一下备份VBOOT系统二

    //执行命令
    fun DoCmd(cmd: String) {
        try {
            val p = Runtime.getRuntime().exec("su")
            val out = DataOutputStream(p.outputStream)
            out.writeBytes(cmd)
            out.writeBytes("\n")
            out.writeBytes("exit\n")
            out.writeBytes("exit\n")
            out.flush()
        } catch (e: IOException) {
            NoRoot()
        }

    }


    //执行命令
    fun DoCmdSync(cmd: String) {
        try {
            val p = Runtime.getRuntime().exec("su")
            val out = DataOutputStream(p.outputStream)
            out.writeBytes(cmd)
            out.writeBytes("\n")
            out.writeBytes("exit\n")
            out.writeBytes("exit\n")
            out.flush()
            p.waitFor()
        } catch (e: IOException) {
            NoRoot()
        } catch (e: Exception) {

        }

    }

    fun GetProp(prop: String, grep: String?): String? {
        try {
            val p = Runtime.getRuntime().exec("sh")
            val out = DataOutputStream(p.outputStream)
            out.writeBytes("if [ ! -f \"$prop\" ]; then echo \"\"; exit 1; fi;\n")
            val cmd = "cat " + prop + if (grep != null && grep.length > 0) " | grep " + grep else ""
            out.writeBytes(cmd)
            out.writeBytes("\n")
            out.writeBytes("exit\n")
            out.flush()
            out.close()

            val bufferedreader = p.inputStream.bufferedReader()

            val stringBuffer = StringBuilder()
            bufferedreader.lineSequence().joinTo(stringBuffer,"\n")
            bufferedreader.close()
            p.destroy()
            return stringBuffer.toString().trim { it <= ' ' }
        } catch (e: Exception) {
            e.stackTrace
        }

        return null
    }
}
