package com.omarea.krscript.executor;

import android.content.Context;

import com.omarea.common.shared.FileWrite;

import java.util.HashMap;

/**
 * Created by Hello on 2018/04/03.
 */

public class ExtractAssets {
    // 用于记录已经提取过的资源，避免重复提取浪费性能
    private static HashMap<String, String> extractHisotry = new HashMap<String, String>();

    private Context context;

    public ExtractAssets(Context context) {
        this.context = context;
    }

    private String extractScript(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        if (extractHisotry.containsKey(fileName)) {
            return extractHisotry.get(fileName);
        }

        if (fileName.startsWith("file:///android_asset/")) {
            fileName = fileName.substring("file:///android_asset/".length());
        }

        String filePath = FileWrite.INSTANCE.writePrivateShellFile(fileName, fileName, context);

        if (filePath != null) {
            extractHisotry.put(fileName, filePath);
        }

        return filePath;
    }

    public String extractResource(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        if (extractHisotry.containsKey(fileName)) {
            return extractHisotry.get(fileName);
        }

        if (fileName.endsWith(".sh")) {
            return extractScript(fileName);
        }
        if (fileName.startsWith("file:///android_asset/")) {
            fileName = fileName.substring("file:///android_asset/".length());
        }
        String filePath = FileWrite.INSTANCE.writePrivateFile(context.getAssets(), fileName, fileName, context);

        if (filePath != null) {
            extractHisotry.put(fileName, filePath);
        }

        return filePath;
    }

    public String extractResources(String dir) {
        if (dir == null || dir.isEmpty()) {
            return null;
        }

        if (extractHisotry.containsKey(dir)) {
            return extractHisotry.get(dir);
        }

        if (dir.startsWith("file:///android_asset/")) {
            dir = dir.substring("file:///android_asset/".length());
        } else if (dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }

        try {
            String[] files = context.getAssets().list(dir);
            if (files != null && files.length > 0) {
                for (String file : files) {
                    String relativePath = dir + "/" + file;
                    extractResources(relativePath);
                }
                String outputDir = getExtractPath(dir);
                extractHisotry.put(dir, outputDir);
                return outputDir;
            } else {
                return extractResource(dir);
            }
        } catch (Exception ignored) {
        }

        return "";
    }

    public String getExtractPath(String file) {
        return FileWrite.INSTANCE.getPrivateFilePath(
                context,
                (file.startsWith("file:///android_asset/") ? (file.substring("file:///android_asset/".length())) : file)
        );
    }
}
