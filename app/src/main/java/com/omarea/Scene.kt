package com.omarea

import android.app.Application
import android.content.Context
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.ShellExecutor
import com.omarea.vtools.R

class Scene : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        ShellExecutor.setExtraEnvPath(
                FileWrite.getPrivateFilePath(this, getString(R.string.toolkit_install_path))
        )
    }
}
