package com.omarea.shared;

import android.content.Context;

import com.omarea.shell.KeepShellPublic;
import com.omarea.shell.RootFile;

import java.io.File;

public class MagiskExtend {
    public static String MAGISK_PATH = "/sbin/.core/img/scene_systemless/";

    public static void magiskModuleInstall(Context context) {
        String moduleProp = "id=scene_systemless\n" +
                "name=Scene的附加模块\n" +
                "version=v1\n" +
                "versionCode=1\n" +
                "author=嘟嘟ski\n" +
                "description=Scene提供的Magisk拓展模块，从而在不修改系统文件的情况下，更改一些参数\n" +
                "minMagisk=17000\n";
        String systemProp = "# This file will be read by resetprop\n" +
                "# Example: Change dpi\n" +
                "# ro.sf.lcd_density=410\n" +
                "# vendor.display.lcd_density=410\n";
        String service = "#!/system/bin/sh\n" +
                "# Please don't hardcode /magisk/modname/... ; instead, please use $MODDIR/...\n" +
                "# This will make your scripts compatible even if Magisk change its mount point in the future\n" +
                "MODDIR=${0%/*}\n" +
                "\n" +
                "# This script will be executed in late_start service mode\n" +
                "# More info in the main Magisk thread\n";
        String fsPostData = "#!/system/bin/sh\n" +
                "# Please don't hardcode /magisk/modname/... ; instead, please use $MODDIR/...\n" +
                "# This will make your scripts compatible even if Magisk change its mount point in the future\n" +
                "MODDIR=${0%/*}\n" +
                "\n" +
                "# This script will be executed in post-fs-data mode\n" +
                "# More info in the main Magisk thread\n";

        writeModuleFile(moduleProp, "module.prop", context);
        writeModuleFile(systemProp, "system.prop", context);
        writeModuleFile(service, "service.sh", context);
        writeModuleFile(fsPostData, "post-fs-data.sh", context);
        writeModuleFile("", "auto_mount", context);
        writeModuleFile("", "update", context);
    }

    private static void writeModuleFile(String text, String name, Context context) {
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
        String magiskVersion = KeepShellPublic.INSTANCE.doCmdSync("magisk -V");
        if (!magiskVersion.equals("error")) {
            try {
                return Integer.parseInt(magiskVersion) > 17000;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    /**
     * 是否已安装模块
     * @return
     */
    public static boolean moduleInstalled() {
        return magiskSupported() && RootFile.INSTANCE.dirExists(MAGISK_PATH );
    }

    public static void setSystemProp(String prop, String value) {
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
        if (RootFile.INSTANCE.itemExists(newfile)) {
            String output = MAGISK_PATH.substring(0, MAGISK_PATH.length() - 1) + (orginPath.startsWith("/vendor") ? ("/system" + orginPath) : orginPath);
            String dir = new File(output).getParent();
            KeepShellPublic.INSTANCE.doCmdSync("mkdir -p \"" + dir + "\"\n" + "cp \"" + newfile + "\" \"" + output + "\"\n" +
                    "chmod 777 \"" + output + "\"" +
                    "\necho '' > " + MAGISK_PATH + "update");
        }
    }

    public static void replaceSystemDir(String orginPath, String newfile) {
        if (RootFile.INSTANCE.itemExists(newfile)) {
            String output = MAGISK_PATH.substring(0, MAGISK_PATH.length() - 1) + (orginPath.startsWith("/vendor") ? ("/system" + orginPath) : orginPath);
            String dir = new File(output).getParent();
            KeepShellPublic.INSTANCE.doCmdSync("mkdir -p \"" + dir + "\"\n" + "cp -a \"" + newfile + "\" \"" + output + "\"\n" +
                    "chmod 777 \"" + output + "\"" +
                    "\necho '' > " + MAGISK_PATH + "update");
        }
    }

    public static void cancelReplace(String orginPath) {
        String output = MAGISK_PATH.substring(0, MAGISK_PATH.length() - 1) + (orginPath.startsWith("/vendor") ? ("/system" + orginPath) : orginPath);
        KeepShellPublic.INSTANCE.doCmdSync("rm -f \""+ output + "\"");
    }

    public static String getProps () {
        if (moduleInstalled()) {
            return KeepShellPublic.INSTANCE.doCmdSync("cat " + MAGISK_PATH + "system.prop");
        }
        return "";
    }

    public static void updateProps(String fromFile) {
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
        if (RootFile.INSTANCE.fileExists(fromFile)) {
            KeepShellPublic.INSTANCE.doCmdSync("cp \"" + fromFile + "\" " + MAGISK_PATH + "post-fs-data.sh\n" +
                    "\nchmod 777 " + MAGISK_PATH + "post-fs-data.sh" +
                    "\necho '' > " + MAGISK_PATH + "update");
        }
    }
}
