package com.omarea.library.shell;

import com.omarea.common.shell.KeepShellPublic;
import com.omarea.common.shell.KernelProrp;
import com.omarea.common.shell.RootFile;
import com.omarea.model.CpuStatus;

import java.util.ArrayList;

public class ThermalControlUtils {
    //Thermal
    private final String thermal_core_control = "/sys/module/msm_thermal/core_control/enabled";//1 0
    private final String thermal_vdd_restriction = "/sys/module/msm_thermal/vdd_restriction/enabled"; //1 0
    private final String thermal_parameters = "/sys/module/msm_thermal/parameters/enabled"; //Y N

    public Boolean isSupported() {
        return RootFile.INSTANCE.itemExists(thermal_core_control) ||
                RootFile.INSTANCE.itemExists(thermal_vdd_restriction) ||
                RootFile.INSTANCE.itemExists(thermal_parameters);
    }

    public String getCoreControlState() {
        return KernelProrp.INSTANCE.getProp(thermal_core_control).trim();
    }

    public void setCoreControlState(Boolean online) {
        String val = online ? "1" : "0";
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 " + thermal_core_control);
        commands.add("echo " + val + " > " + thermal_core_control);
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }

    public String getVDDRestrictionState() {
        return KernelProrp.INSTANCE.getProp(thermal_vdd_restriction).trim();
    }

    public void setVDDRestrictionState(Boolean online) {
        String val = online ? "1" : "0";
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 " + thermal_vdd_restriction);
        commands.add("echo " + val + " > " + thermal_vdd_restriction);
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }

    public String getTheramlState() {
        return KernelProrp.INSTANCE.getProp(thermal_parameters).trim();
    }

    public void setTheramlState(Boolean online) {
        String val = online ? "Y" : "N";
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 " + thermal_parameters);
        commands.add("echo " + val + " > " + thermal_parameters);
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }


    public ArrayList<String> buildSetThermalParams(CpuStatus cpuStatus, ArrayList<String> commands) {
        if (!(cpuStatus.coreControl == null || cpuStatus.coreControl.isEmpty())) {
            commands.add("chmod 0664 " + thermal_core_control);
            commands.add("echo " + cpuStatus.coreControl + " > " + thermal_core_control);
        }
        if (!(cpuStatus.vdd == null || cpuStatus.vdd.isEmpty())) {
            commands.add("chmod 0664 " + thermal_vdd_restriction);
            commands.add("echo " + cpuStatus.vdd + " > " + thermal_vdd_restriction);
        }
        if (!(cpuStatus.msmThermal == null || cpuStatus.msmThermal.isEmpty())) {
            commands.add("chmod 0664 " + thermal_parameters);
            commands.add("echo " + cpuStatus.msmThermal + " > " + thermal_parameters);
        }
        return commands;
    }
}
