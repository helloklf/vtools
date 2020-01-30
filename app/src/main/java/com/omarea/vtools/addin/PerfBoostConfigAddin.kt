package com.omarea.vtools.addin

import android.content.Context
import android.os.Build
import android.widget.Toast
import com.omarea.common.shared.FileWrite
import com.omarea.common.shared.MagiskExtend
import com.omarea.common.ui.DialogHelper
import com.omarea.shell_utils.PlatformUtils
import com.omarea.utils.CommonCmds

/**
 * Created by Hello on 2018/03/22.
 */

class PerfBoostConfigAddin(private var context: Context) : AddinBase(context) {
    fun install() {
        val soc = PlatformUtils().getCPUName()
        if (soc == "msm8998") {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Toast.makeText(context, "只支持Android 8.0以后的系统！", Toast.LENGTH_SHORT).show()
            } else {
                msm8898()
            }
        } else if (soc == "sm6150") {
            sm6150()
        } else if (soc == "msmnile") {
            msmnile()
        } else if (soc == "sdm845") {
            sdm845()
        } else if (soc == "sdm630") {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Toast.makeText(context, "只支持Android 8.0以后的系统！", Toast.LENGTH_SHORT).show()
            } else {
                sdm630()
            }
        } else if (soc == "sdm660") {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Toast.makeText(context, "只支持Android 8.0以后的系统！", Toast.LENGTH_SHORT).show()
            } else {
                sdm660()
            }
        } else {
            DialogHelper.helpInfo(context, "暂未适配这个处理器，暂时只支持 骁龙835、845、855、660AIE、630、730！")
        }
    }

    private fun msmnile() {
        val path = FileWrite.writePrivateFile(context.assets, "addin/perfboostsconfig_msmnile.xml", "perfboostsconfig_msmnile.xml", context)
        copyFile(path)
    }

    private fun sdm845() {
        val path = FileWrite.writePrivateFile(context.assets, "addin/perfboostsconfig_sdm845.xml", "perfboostsconfig_sdm845.xml", context)
        copyFile(path)
    }

    private fun msm8898() {
        val path = FileWrite.writePrivateFile(context.assets, "addin/perfboostsconfig_msm8998.xml", "perfboostsconfig_msm8998.xml", context)
        copyFile(path)
    }

    // 配置文件和660AIE的写在一起了
    private fun sdm630() {
        val path = FileWrite.writePrivateFile(context.assets, "addin/perfboostsconfig_sdm660.xml", "perfboostsconfig_sdm660.xml", context)
        copyFile(path)
    }

    private fun sm6150() {
        val path = FileWrite.writePrivateFile(context.assets, "addin/perfboostsconfig_sm6150.xml", "perfboostsconfig_sm6150.xml", context)
        copyFile(path)
    }

    private fun sdm660() {
        val path = FileWrite.writePrivateFile(context.assets, "addin/perfboostsconfig_sdm660.xml", "perfboostsconfig_sdm660.xml", context)
        copyFile(path)
    }

    private fun copyFile(path: String?) {
        if (path != null) {
            val installPath = "/system/vendor/etc/perf/perfboostsconfig.xml"
            if (MagiskExtend.moduleInstalled()) {
                MagiskExtend.replaceSystemFile(installPath, path)

                DialogHelper.helpInfo(context, "已通过Magisk更改参数，请重启手机~")
            } else {
                command = StringBuilder()
                        .append(CommonCmds.MountSystemRW)
                        .append("if [[ ! -e $installPath.bak ]]; then cp $installPath $installPath.bak; fi;\n")
                        .append("rm -f $installPath\n")
                        .append("cp $path $installPath\n")
                        .append("chmod 755 $installPath\n")
                        .append("verify=`busybox md5sum $path | cut -f1 -d ' '`\n")
                        .append("md5=`busybox md5sum $installPath | cut -f1 -d ' '`\n")
                        .append("if [[ \$md5 = \$verify ]]; then exit 0; else exit 1; fi;\n")
                        .toString()

                super.run()
            }
        } else {
            DialogHelper.helpInfo(context, "配置文件提取失败！")
        }
    }
}
