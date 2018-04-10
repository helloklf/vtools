package com.omarea.shell.cpucontrol;

import android.content.Context;
import android.widget.Toast;

import com.omarea.shell.SysUtils;

import java.io.File;
import java.util.ArrayList;

public class ThermalControlUtils {
    public static Boolean isSupported () {
        return new File(Constants.thermal_core_control).exists() ||
                new File(Constants.thermal_vdd_restriction).exists() ||
                new File(Constants.thermal_parameters).exists();
    }

    public static String getCoreControlState() {
        return SysUtils.readOutputFromFile(Constants.thermal_core_control).trim();
    }

    public static String getVDDRestrictionState() {
        return SysUtils.readOutputFromFile(Constants.thermal_vdd_restriction).trim();
    }

    public static String getTheramlState() {
        return SysUtils.readOutputFromFile(Constants.thermal_parameters).trim();
    }

    public static void setCoreControlState(Boolean online, Context context) {
        String val = online ? "1" : "0";
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 " + Constants.thermal_core_control);
        commands.add("echo " + val + " > " + Constants.thermal_core_control);

        boolean success = SysUtils.executeRootCommand(commands);
        if (success) {
           Toast.makeText(context, "OK!", Toast.LENGTH_SHORT).show();
        }
    }

    public static void setVDDRestrictionState(Boolean online, Context context) {
        String val = online ? "1" : "0";
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 " + Constants.thermal_vdd_restriction);
        commands.add("echo " + val + " > " + Constants.thermal_vdd_restriction);

        boolean success = SysUtils.executeRootCommand(commands);
        if (success) {
           Toast.makeText(context, "OK!", Toast.LENGTH_SHORT).show();
        }
    }

    public static void setTheramlState(Boolean online, Context context) {
        String val = online ? "Y" : "N";
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 " + Constants.thermal_parameters);
        commands.add("echo " + val + " > " + Constants.thermal_parameters);

        boolean success = SysUtils.executeRootCommand(commands);
        if (success) {
           Toast.makeText(context, "OK!", Toast.LENGTH_SHORT).show();
        }
    }
}
