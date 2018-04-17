package com.omarea.scripts;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

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
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            String filePath = context.getFilesDir().getAbsolutePath() + "/" + fileName;
            File dir = new File(filePath).getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            byte[] cache = new byte[4096];
            int length;
            while ((length = inputStream.read(cache)) > 0) {
                fileOutputStream.write(cache, 0, length);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            inputStream.close();
            return filePath;
        } catch (Exception ex) {
            return null;
        }
    }
}
