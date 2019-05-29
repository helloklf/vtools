package com.omarea.shell.cpucontrol;

import android.content.Context;
import android.widget.Toast;

import com.omarea.common.shell.KernelProrp;
import com.omarea.common.shell.RootFile;
import com.omarea.shell.SuDo;

import java.util.ArrayList;

public class ThermalControlUtils {
    public static Boolean isSupported() {
        return RootFile.INSTANCE.itemExists(Constants.thermal_core_control) ||
                RootFile.INSTANCE.itemExists(Constants.thermal_vdd_restriction) ||
                RootFile.INSTANCE.itemExists(Constants.thermal_parameters);
    }

    public static String getCoreControlState() {
        return KernelProrp.INSTANCE.getProp(Constants.thermal_core_control).trim();
    }

    public static String getVDDRestrictionState() {
        return KernelProrp.INSTANCE.getProp(Constants.thermal_vdd_restriction).trim();
    }

    public static String getTheramlState() {
        return KernelProrp.INSTANCE.getProp(Constants.thermal_parameters).trim();
    }

    public static void setCoreControlState(Boolean online, Context context) {
        String val = online ? "1" : "0";
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 " + Constants.thermal_core_control);
        commands.add("echo " + val + " > " + Constants.thermal_core_control);

        boolean success = SuDo.Companion.execCmdSync(commands);
        if (success) {
            Toast.makeText(context, "OK!", Toast.LENGTH_SHORT).show();
        }
    }

    public static void setVDDRestrictionState(Boolean online, Context context) {
        String val = online ? "1" : "0";
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 " + Constants.thermal_vdd_restriction);
        commands.add("echo " + val + " > " + Constants.thermal_vdd_restriction);

        boolean success = SuDo.Companion.execCmdSync(commands);
        if (success) {
            Toast.makeText(context, "OK!", Toast.LENGTH_SHORT).show();
        }
    }

    public static void setTheramlState(Boolean online, Context context) {
        String val = online ? "Y" : "N";
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 " + Constants.thermal_parameters);
        commands.add("echo " + val + " > " + Constants.thermal_parameters);

        boolean success = SuDo.Companion.execCmdSync(commands);
        if (success) {
            Toast.makeText(context, "OK!", Toast.LENGTH_SHORT).show();
        }
    }
}
