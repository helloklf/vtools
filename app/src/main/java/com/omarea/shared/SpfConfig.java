package com.omarea.shared;

/**
 * Created by helloklf on 2017/11/02.
 */

public class SpfConfig {
    public static String POWER_CONFIG_SPF = "powercfg";

    public static String CHARGE_SPF = "charge"; //spf
    public static String CHARGE_SPF_QC_BOOSTER = "qc_booster"; //bool
    public static String CHARGE_SPF_QC_LIMIT = "charge_limit_ma"; //int
    public static String CHARGE_SPF_BP = "bp"; //bool
    public static String CHARGE_SPF_BP_LEVEL = "bp_level"; //int

    public static String BOOSTER_CONFIG_SPF = "boostercfg";
    public static String BOOSTER_SPF_CLEAR_CACHE = "auto_clear_cache";
    public static String BOOSTER_SPF_DOZE_MOD = "use_doze_mod";

    public static String GLOBAL_SPF = "global"; //spf
    public static String GLOBAL_SPF_AUTO_INSTALL = "is_auto_install";
    public static String GLOBAL_SPF_AUTO_BOOSTER = "is_auto_booster";
    public static String GLOBAL_SPF_DYNAMIC_CPU = "is_dynamic_cpu";
    public static String GLOBAL_SPF_DYNAMIC_CPU_CONFIG = "dynamic_cpu_config";
    public static String GLOBAL_SPF_DEBUG = "is_debug";
    public static String GLOBAL_SPF_DELAY = "is_delay_start";

    public static String SWAP_SPF = "swap";
    public static String SWAP_SPF_SWAP = "swap";
    public static String SWAP_SPF_SWAP_SIZE = "swap_size";
    public static String SWAP_SPF_SWAP_FIRST = "swap_first";
    public static String SWAP_SPF_ZRAM = "zram";
    public static String SWAP_SPF_ZRAM_SIZE = "zram_size";
    public static String SWAP_SPF_SWAPPINESS = "swappiness";
}
