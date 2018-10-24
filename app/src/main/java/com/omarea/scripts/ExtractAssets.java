package com.omarea.scripts;

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

    public String extractToFilesDir(String fileName) {
        if (fileName.startsWith("file:///android_asset/")) {
            fileName = fileName.substring("file:///android_asset/".length());
        }
        return FileWrite.INSTANCE.writePrivateShellFile(fileName, fileName, context);
    }
}
