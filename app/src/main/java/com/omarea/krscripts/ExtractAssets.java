package com.omarea.krscripts;

import android.content.Context;

import com.omarea.shared.FileWrite;

/**
 * Created by Hello on 2018/04/03.
 */

public class ExtractAssets {
    private Context context;

    public ExtractAssets(Context context) {
        this.context = context;
    }

    public String extractScript(String fileName) {
        if (fileName.startsWith("file:///android_asset/")) {
            fileName = fileName.substring("file:///android_asset/".length());
        }
        return FileWrite.INSTANCE.writePrivateShellFile(fileName, fileName, context);
    }

    public String extractResource(String fileName) {
        if (fileName.endsWith(".sh")) {
            return extractScript(fileName);
        }
        if (fileName.startsWith("file:///android_asset/")) {
            fileName = fileName.substring("file:///android_asset/".length());
        }
        return FileWrite.INSTANCE.writePrivateFile(context.getAssets(), fileName, fileName, context);
    }
}
