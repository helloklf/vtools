package com.omarea.shell;

import android.os.Environment;
import android.os.StatFs;

/**
 * Created by Hello on 2017/11/01.
 */

public class Files {
    public static long GetDirFreeSizeMB(String dir) {
        StatFs stat = new StatFs(dir);
        long size = stat.getAvailableBytes();
        return size / 1024 / 1024; //剩余空间
    }
}
