package com.omarea.shell_utils

import android.content.Context
import android.os.Build
import com.omarea.common.shared.FileWrite.getPrivateFilePath
import com.omarea.common.shared.FileWrite.writePrivateFile
import com.omarea.vtools.R
import java.io.File
import java.util.*

class ToyboxIntaller(private val context: Context) {
    public fun install() : String {

        val installPath: String = context.getString(R.string.toolkit_install_path)
        val abi = Build.SUPPORTED_ABIS.joinToString(" ").toLowerCase(Locale.getDefault())
        val fileName = if (abi.contains("arm64")) "toybox-outside64" else "toybox-outside";
        val toyboxInstallPath = "$installPath/$fileName"
        val outsideToybox = getPrivateFilePath(context, toyboxInstallPath)

        if (!File(outsideToybox).exists()) {
            writePrivateFile(context.getAssets(), toyboxInstallPath, toyboxInstallPath, context)
        }

        return outsideToybox
    }
}