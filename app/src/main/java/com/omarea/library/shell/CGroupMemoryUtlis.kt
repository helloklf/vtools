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
        private var memcgShell: String? = null
        public val inited: Boolean
            get () {
                return memcgShell != null
            }
    }

    public val isSupported: Boolean
        get() {
            if (supported == null) {
                supported = RootFile.fileExists("/dev/memcg/tasks") || RootFile.fileExists("/sys/fs/cgroup/memory/tasks")
            }
            return supported == true
        }

    public fun init() {
        // memcgShell 为null为未初始化或初始化失败状态，需要先执行初始化
        if (memcgShell == null && isSupported) {
            val initShell = RawText.getRawText(context, R.raw.memcg_set_init).toByteArray(Charset.defaultCharset())
            val execShell = RawText.getRawText(context, R.raw.memcg_set).toByteArray(Charset.defaultCharset())
            val initOutName = "memcg_set_init.sh"
            val execOutName = "memcg_set.sh"
            if (
                    FileWrite.writePrivateFile(initShell, initOutName, context) &&
                    FileWrite.writePrivateFile(execShell, execOutName, context)
            ) {
                val shellPath = FileWrite.getPrivateFilePath(context, initOutName)
                KeepShellPublic.doCmdSync("sh $shellPath")
                val fullExecPath = FileWrite.getPrivateFilePath(context, execOutName)
                memcgShell = "sh $fullExecPath '%s' '%s' > /dev/null 2>&1 &"
            }
        }
    }

    public fun setGroup(packageName: String, group: String) {
        if (!isSupported) {
            return
        }

        init()

        if (memcgShell != null) {
            val groupPath = (if (group.isNotEmpty()) {
                ("/$group")
            } else {
                ""
            })
            val cmd = String.format(memcgShell!!, packageName, groupPath)
            KeepShellPublic.doCmdSync(cmd)
        } else {
            Log.e("Scene", "CGroup Init Fail!")
            return
        }
    }
}