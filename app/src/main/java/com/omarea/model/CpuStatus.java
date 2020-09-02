package com.omarea.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Hello on 2018/08/04.
 */

public class CpuStatus implements Serializable {
    public ArrayList<CpuClusterStatus> cpuClusterStatuses = new ArrayList<>();

    public String coreControl = "";
    public String vdd = "";
    public String msmThermal = "";
    public ArrayList<Boolean> coreOnline = null;

    public int exynosHmpUP = 0;
    public int exynosHmpDown = 0;
    public boolean exynosHmpBooster = false;
    public boolean exynosHotplug = false;

    public String adrenoMinFreq = "";
    public String adrenoMaxFreq = "";
    public String adrenoMinPL = "";
    public String adrenoMaxPL = "";
    public String adrenoDefaultPL = "";
    public String adrenoGovernor = "";

    public String cpusetBackground = "";
    public String cpusetSysBackground = "";
    public String cpusetForeground = "";
    public String cpusetTopApp = "";
    public String cpusetRestricted = "";
}
