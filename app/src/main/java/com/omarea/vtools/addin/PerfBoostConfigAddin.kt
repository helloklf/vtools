package com.omarea.vtools.addin

import android.content.Context
import android.os.Build
import android.widget.Toast
import com.omarea.shared.CommonCmds
import com.omarea.shared.FileWrite
import com.omarea.shell.Platform
import com.omarea.shell.cpucontrol.Constants

/**
 * Created by Hello on 2018/03/22.
 */

class PerfBoostConfigAddin(private var context: Context) : AddinBase(context) {
    fun install () {
        val soc = Platform().getCPUName()
        if (soc == "msm8998") {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Toast.makeText(context, "只支持Android 8.0以后的系统！", Toast.LENGTH_SHORT).show()
                return
            }
            msm8898()
        } else {
            Toast.makeText(context, "暂未适配这个处理器，暂时只支持 骁龙835！", Toast.LENGTH_SHORT).show()
        }
    }
    private fun msm8898() {
        val path = FileWrite.writePrivateFile(context.assets, "addin/perfboostsconfig_msm8998.xml", "perfboostsconfig_msm8998.xml", context)
        if (path != null) {
            val installPath = "/system/vendor/etc/perf/perfboostsconfig.xml"
            command = StringBuilder()
                    .append(CommonCmds.MountSystemRW)
                    .append(CommonCmds.MountVendorRW)
                    .append("if [[ ! -e $installPath.bak ]]; then cp $installPath $installPath.bak; fi;\n")
                    .append("rm -f $installPath\n")
                    .append("cp $path $installPath\n")
                    .append("chmod 644 $installPath\n")
                    .append("md5=`busybox md5sum $installPath | cut -f1 -d ' '`\n")
                    .append("if [[ \$md5 = 'ff0b967ccccd058773f5ccc44c837442' ]]; then exit 0; else exit 1; fi;\n")
                    .toString()

            super.run()
        } else {
            Toast.makeText(context, "配置文件perfboostsconfig_msm8998.xml提取失败！", Toast.LENGTH_SHORT).show()
        }
    }
}
