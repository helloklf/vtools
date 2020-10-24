package com.omarea.library.shell

import android.content.Context
import com.omarea.common.shared.RawText
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.RootFile
import com.omarea.vtools.R

public class CGroupMemoryUtlis(private val context: Context) {
    companion object {
        private var supported: Boolean? = null
    }

    public val isSupported: Boolean
        get () {
            if (supported == null) {
                supported = RootFile.fileExists("/dev/memcg/tasks") || RootFile.fileExists("/sys/fs/cgroup/memory/tasks")
            }
            return supported == true
        }

    private var memcgShell: String? = null
    public fun setGroup(packageName: String, group: String) {
        if (memcgShell == null) {
            memcgShell = RawText.getRawText(context, R.raw.memcg_set)
        }

        if (memcgShell != null) {
            KeepShellPublic.doCmdSync(String.format(memcgShell!!, packageName, group))
        }
    }
}