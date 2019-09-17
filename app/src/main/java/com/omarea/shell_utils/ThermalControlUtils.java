package com.omarea.shell_utils;

import android.content.Context;
import android.widget.Toast;

import com.omarea.common.shell.KeepShellPublic;
import com.omarea.common.shell.KernelProrp;
import com.omarea.common.shell.RootFile;

import java.util.ArrayList;

public class ThermalControlUtils {
    //Thermal
    private final static String thermal_core_control = "/sys/module/msm_thermal/core_control/enabled";//1 0
    private final static String thermal_vdd_restriction = "/sys/module/msm_thermal/vdd_restriction/enabled"; //1 0
    private final static String thermal_parameters = "/sys/module/msm_thermal/parameters/enabled"; //Y N

    public static Boolean isSupported() {
        return RootFile.INSTANCE.itemExists(thermal_core_control) ||
                RootFile.INSTANCE.itemExists(thermal_vdd_restriction) ||
                RootFile.INSTANCE.itemExists(thermal_parameters);
    }

    public static String getCoreControlState() {
        return KernelProrp.INSTANCE.getProp(thermal_core_control).trim();
    }

    public static String getVDDRestrictionState() {
        return KernelProrp.INSTANCE.getProp(thermal_vdd_restriction).trim();
    }

    public static String getTheramlState() {
        return KernelProrp.INSTANCE.getProp(thermal_parameters).trim();
    }

    public static void setCoreControlState(Boolean online, Context context) {
        String val = online ? "1" : "0";
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 " + thermal_core_control);
        commands.add("echo " + val + " > " + thermal_core_control);

        boolean success = KeepShellPublic.INSTANCE.doCmdSync(commands);
        if (success) {
            Toast.makeText(context, "OK!", Toast.LENGTH_SHORT).show();
        }
    }

    public static void setVDDRestrictionState(Boolean online, Context context) {
        String val = online ? "1" : "0";
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 " + thermal_vdd_restriction);
        commands.add("echo " + val + " > " + thermal_vdd_restriction);

        boolean success = KeepShellPublic.INSTANCE.doCmdSync(commands);
        if (success) {
            Toast.makeText(context, "OK!", Toast.LENGTH_SHORT).show();
        }
    }

    public static void setTheramlState(Boolean online, Context context) {
        String val = online ? "Y" : "N";
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 " + thermal_parameters);
        commands.add("echo " + val + " > " + thermal_parameters);

        boolean success = KeepShellPublic.INSTANCE.doCmdSync(commands);
        if (success) {
            Toast.makeText(context, "OK!", Toast.LENGTH_SHORT).show();
        }
    }
}
