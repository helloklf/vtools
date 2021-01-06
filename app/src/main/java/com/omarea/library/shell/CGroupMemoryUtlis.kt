package com.omarea.library.shell

import android.content.Context
import android.util.Log
import com.omarea.common.shared.FileWrite
import com.omarea.common.shared.RawText
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.RootFile
import com.omarea.vtools.R
import java.nio.charset.Charset

public class CGroupMemoryUtlis(private val context: Context) {
    companion object {
        private var supported: Boolean? = null
        private var init: Boolean = false
    }

    public val isSupported: Boolean
        get() {
            if (supported == null) {
                supported = RootFile.fileExists("/dev/memcg/tasks") || RootFile.fileExists("/sys/fs/cgroup/memory/tasks")
            }
            return supported == true
        }

    private var memcgShell: String? = null
    public fun setGroup(packageName: String, group: String) {
        if (!init) {
            val initShell = RawText.getRawText(context, R.raw.memcg_set_init)
            val outName = "memcg_set_init.sh"
            if (FileWrite.writePrivateFile(initShell.toByteArray(Charset.defaultCharset()), outName, context)) {
                val shellPath = FileWrite.getPrivateFilePath(context, outName)
                KeepShellPublic.doCmdSync("sh $shellPath")
                init = true
            }
        }
        if (!init) {
            Log.e("Scene", "CGroup Init Fail!")
            return
        }
        if (memcgShell == null) {
            memcgShell = RawText.getRawText(context, R.raw.memcg_set)
        }

        if (memcgShell != null) {
            val groupPath = (if (group.isNotEmpty()) {
                ("/$group")
            } else {
                ""
            })
            KeepShellPublic.doCmdSync(String.format(memcgShell!!, packageName, groupPath))
        }
    }
}