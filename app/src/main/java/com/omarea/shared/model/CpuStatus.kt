package com.omarea.shared.model

/**
 * Created by Hello on 2018/08/03.
 */

//Parcelable
class CpuStatus {
    var cluster_little_min_freq = "";
    var cluster_little_max_freq = "";
    var cluster_little_governor = "";

    var cluster_big_min_freq = "";
    var cluster_big_max_freq = "";
    var cluster_big_governor = "";

    var coreControl = ""
    var vdd = ""
    var msmThermal = ""
    var boost = ""
    var boostFreq = ""
    var boostTime = ""
    var coreOnline = arrayListOf<Boolean>()

    var exynosHmpUP = 0;
    var exynosHmpDown = 0;
    var exynosHmpBooster = false;
    var exynosHotplug = false;

    var adrenoMinFreq = ""
    var adrenoMaxFreq = ""
    var adrenoMinPL = ""
    var adrenoMaxPL = ""
    var adrenoDefaultPL = ""
    var adrenoGovernor = ""
}
