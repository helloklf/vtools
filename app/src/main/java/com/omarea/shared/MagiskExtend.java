package com.omarea.shared;

import android.content.Context;

import com.omarea.shell.KeepShellPublic;
import com.omarea.shell.RootFile;

import java.io.File;

public class MagiskExtend {
    // source /data/adb/util_functions.sh

    public static String MAGISK_PATH = "/sbin/.core/img/scene_systemless/";
    private static String MAGISK_MODULE_NAME = "scene_systemless";
    private static String MAGISK_ROOT_PATH1 = "/sbin/.core/img";
    private static String MAGISK_ROOT_PATH2 = "/sbin/.magisk/img";
    private static int supported = -1;

    // 递归方式 计算文件的大小
    private static long getTotalSizeOfFilesInDir(final File file) {
        if (file.isFile())
            return file.length();
        final File[] children = file.listFiles();
        long total = 0;
        if (children != null)
            for (final File child : children)
                total += getTotalSizeOfFilesInDir(child);
        return total;
    }

    /**
     * 自动调整镜像大小
     * @param require
     */
    private static void resizeMagiskImg(long require) {
        long currentSize = new File(MAGISK_PATH).getTotalSpace();
        long space = new File(MAGISK_PATH).getFreeSpace();
        if (space < (require + 2097152)) {
            long sizeMB = ((currentSize - space + require) / 1024 / 1024) + 2;
            KeepShellPublic.INSTANCE.doCmdSync("imgtool resize /data/adb/magisk.img " + sizeMB);
        }
    }

    public static void magiskModuleInstall(Context context) {
        if (!RootFile.INSTANCE.fileExists("/data/adb/magisk_merge.img")) {
            KeepShellPublic.INSTANCE.doCmdSync("imgtool create /data/adb/magisk_merge.img 64");
        }
        if (!RootFile.INSTANCE.fileExists("/data/adb/magisk.img")) {
            KeepShellPublic.INSTANCE.doCmdSync("imgtool create /data/adb/magisk.img 64");
        }

        String moduleProp = "id=scene_systemless\n" +
                "name=Scene的附加模块\n" +
                "version=v1\n" +
                "versionCode=1\n" +
                "author=嘟嘟ski\n" +
                "description=Scene提供的Magisk拓展模块，从而在不修改系统文件的情况下，更改一些参数\n" +
                "minMagisk=17000\n";
        String systemProp = "# This file will be read by resetprop\n" +
                "# 示例: 更改 dpi\n" +
                "# ro.sf.lcd_density=410\n" +
                "# vendor.display.lcd_density=410\n";
        String service = "#!/system/bin/sh\n" +
                "# 请不要硬编码/magisk/modname/...;相反，请使用$MODDIR/...\n" +
                "# 这将使您的脚本兼容，即使Magisk以后改变挂载点\n" +
                "MODDIR=${0%/*}\n" +
                "\n" +
                "# 该脚本将在设备开机后作为延迟服务启动\n";
        String fsPostData = "#!/system/bin/sh\n" +
                "# 请不要硬编码/magisk/modname/...;相反，请使用$MODDIR/...\n" +
                "# 这将使您的脚本兼容，即使Magisk以后改变挂载点\n" +
                "MODDIR=${0%/*}\n" +
                "\n" +
                "# 此脚本将在post-fs-data模式下执行\n";

        KeepShellPublic.INSTANCE.doCmdSync("mkdir -p /dev/tmp/magisk_scene\n" +
                "imgtool mount /data/adb/magisk_merge.img /dev/tmp/magisk_scene\n");
        MAGISK_PATH = "/dev/tmp/magisk_scene/scene_systemless/";

        writeModuleFile(moduleProp, "module.prop", context);
        writeModuleFile(systemProp, "system.prop", context);
        writeModuleFile(service, "service.sh", context);
        writeModuleFile(fsPostData, "post-fs-data.sh", context);
        writeModuleFile("", "auto_mount", context);
        writeModuleFile("", "update", context);
    }

    private static void writeModuleFile(String text, String name, Context context) {
        resizeMagiskImg(text.length());

        if (!moduleInstalled()) {
            KeepShellPublic.INSTANCE.doCmdSync("mkdir -p " + MAGISK_PATH);
        }
        if (FileWrite.INSTANCE.writePrivateFile(text.getBytes(), name, context)) {
            String path = FileWrite.INSTANCE.getPrivateFilePath(context, name);
            String output = MAGISK_PATH + name;
            KeepShellPublic.INSTANCE.doCmdSync("cp " + path + " " + output + "\nchmod 777 " + output);
        }
    }

    /**
     * 是否已经安装magisk并且版本合适
     *
     * @return 是否已安装
     */
    public static boolean magiskSupported() {
        if (supported == -1) {
            String magiskVersion = KeepShellPublic.INSTANCE.doCmdSync("magisk -V");
            if (!magiskVersion.equals("error")) {
                try {
                    if (RootFile.INSTANCE.dirExists(MAGISK_ROOT_PATH1)) {
                        MAGISK_PATH = MAGISK_ROOT_PATH1 + "/" + MAGISK_MODULE_NAME + "/";
                    } else if (RootFile.INSTANCE.dirExists(MAGISK_ROOT_PATH2)) {
                        MAGISK_PATH = MAGISK_ROOT_PATH2 + "/" + MAGISK_MODULE_NAME + "/";
                    }

                    supported = Integer.parseInt(magiskVersion) >= 17000 ? 1 : 0;
                } catch (Exception ignored) {
                }
            } else {
                supported = 0;
            }
        }
        return supported == 1;
    }

    /**
     * 是否已安装模块
     * @return
     */
    public static boolean moduleInstalled() {
        return magiskSupported() && RootFile.INSTANCE.dirExists(MAGISK_PATH );
    }

    public static void setSystemProp(String prop, String value) {
        resizeMagiskImg(prop.length() + 4 + value.length());

        KeepShellPublic.INSTANCE.doCmdSync(
            "sed -i '/" + prop + "=/'d " +  MAGISK_PATH + "system.prop\n" +
            "echo " + prop + "=\"" + value  +"\" >> " + MAGISK_PATH + "system.prop\n" +
            "echo '' > " + MAGISK_PATH + "update");
    }

    public static void deleteSystemPath(String orginPath) {
        if (RootFile.INSTANCE.itemExists(orginPath)) {
            String output = MAGISK_PATH.substring(0, MAGISK_PATH.length() - 1) + (orginPath.startsWith("/vendor") ? ("/system" + orginPath) : orginPath);
            String dir = new File(output).getParent();
            KeepShellPublic.INSTANCE.doCmdSync("mkdir -p \"" + dir + "\"\necho '' > \"" + output + "\"" +
                    "\necho '' > " + MAGISK_PATH + "update");
        }
    }

    public static void replaceSystemFile(String orginPath, String newfile) {
        resizeMagiskImg(getTotalSizeOfFilesInDir(new File(newfile)));

        if (RootFile.INSTANCE.itemExists(newfile)) {
            String output = getMagiskReplaceFilePath(orginPath);
            String dir = new File(output).getParent();
            KeepShellPublic.INSTANCE.doCmdSync(
                    "mkdir -p \"" + dir + "\"\n" +
                    "cp \"" + newfile + "\" \"" + output + "\"\n" +
                    "chmod 777 \"" + output + "\"" +
                    "\necho '' > " + MAGISK_PATH + "update");
        }
    }

    public static String getMagiskReplaceFilePath(String systemPath) {
        return MAGISK_PATH.substring(0, MAGISK_PATH.length() - 1) + (systemPath.startsWith("/vendor") ? ("/system" + systemPath) : systemPath);
    }

    public static void replaceSystemDir(String orginPath, String newfile) {
        resizeMagiskImg(getTotalSizeOfFilesInDir(new File(newfile)));

        if (RootFile.INSTANCE.itemExists(newfile)) {
            String output = getMagiskReplaceFilePath(orginPath);
            String dir = new File(output).getParent();
            KeepShellPublic.INSTANCE.doCmdSync("mkdir -p \"" + dir + "\"\n" + "cp -a \"" + newfile + "\" \"" + output + "\"\n" +
                    "chmod 777 \"" + output + "\"" +
                    "\necho '' > " + MAGISK_PATH + "update");
        }
    }

    public static void cancelReplace(String orginPath) {
        String output = getMagiskReplaceFilePath(orginPath);
        KeepShellPublic.INSTANCE.doCmdSync("rm -f \""+ output + "\"");
    }

    public static String getProps () {
        if (moduleInstalled()) {
            return KeepShellPublic.INSTANCE.doCmdSync("cat " + MAGISK_PATH + "system.prop");
        }
        return "";
    }

    public static void updateProps(String fromFile) {
        resizeMagiskImg(getTotalSizeOfFilesInDir(new File(fromFile)));

        if (RootFile.INSTANCE.fileExists(fromFile)) {
            KeepShellPublic.INSTANCE.doCmdSync("cp \"" + fromFile + "\" " + MAGISK_PATH + "system.prop\n" +
                    "\nchmod 777 " + MAGISK_PATH + "service.sh" +
                    "\necho '' > " + MAGISK_PATH + "update");
        }
    }

    public static String getServiceSH () {
        if (moduleInstalled()) {
            return KeepShellPublic.INSTANCE.doCmdSync("cat " + MAGISK_PATH + "service.sh");
        }
        return "";
    }

    public static void updateServiceSH(String fromFile) {
        resizeMagiskImg(getTotalSizeOfFilesInDir(new File(fromFile)));

        if (RootFile.INSTANCE.fileExists(fromFile)) {
            KeepShellPublic.INSTANCE.doCmdSync("cp \"" + fromFile + "\" " + MAGISK_PATH + "service.sh\n" +
                    "\nchmod 777 " + MAGISK_PATH + "service.sh" +
                    "\necho '' > " + MAGISK_PATH + "update");
        }
    }

    public static String getFsPostDataSH () {
        if (moduleInstalled()) {
            return KeepShellPublic.INSTANCE.doCmdSync("cat " + MAGISK_PATH + "post-fs-data.sh");
        }
        return "";
    }

    public static void updateFsPostDataSH(String fromFile) {
        resizeMagiskImg(getTotalSizeOfFilesInDir(new File(fromFile)));

        if (RootFile.INSTANCE.fileExists(fromFile)) {
            KeepShellPublic.INSTANCE.doCmdSync("cp \"" + fromFile + "\" " + MAGISK_PATH + "post-fs-data.sh\n" +
                    "\nchmod 777 " + MAGISK_PATH + "post-fs-data.sh" +
                    "\necho '' > " + MAGISK_PATH + "update");
        }
    }
}
