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

    public static String BOOSTER_BLACKLIST_SPF = "boostercfg";

    public static String BOOSTER_SPF_CFG_SPF = "boostercfg2";
    public static String BOOSTER_SPF_CFG_SPF_CLEAR_CACHE = "auto_clear_cache";
    public static String BOOSTER_SPF_CFG_SPF_DOZE_MOD = "use_doze_mod";
    public static String BOOSTER_SPF_CFG_SPF_CLEAR_TASKS = "auto_clear_tasks";

    public static String DATA = "data";
    public static String WIFI = "wifi";
    public static String NFC = "nfc";
    public static String GPS = "gps";

    public static String ON = "_on";
    public static String OFF = "_off";

    public static String GLOBAL_SPF = "global"; //spf
    public static String GLOBAL_SPF_AUTO_INSTALL = "is_auto_install";
    public static String GLOBAL_SPF_AUTO_BOOSTER = "is_auto_booster";
    public static String GLOBAL_SPF_DYNAMIC_CPU = "is_dynamic_cpu";
    public static String GLOBAL_SPF_DYNAMIC_CPU_CONFIG = "dynamic_cpu_config";
    public static String GLOBAL_SPF_DEBUG = "is_debug";
    public static String GLOBAL_SPF_START_DELAY = "start_delay";
    public static String GLOBAL_SPF_DELAY = "is_delay_start";
    public static String GLOBAL_SPF_NOTIFY = "accessbility_notify";
    public static String GLOBAL_SPF_AUTO_REMOVE_RECENT = "remove_recent";
    public static String GLOBAL_SPF_NIGHT_MODE = "app_night_mode";
    public static String GLOBAL_SPF_MAC = "wifi_mac";
    public static String GLOBAL_SPF_MAC_AUTOCHANGE = "wifi_mac_autochange";

    public static String SWAP_SPF = "swap"; //spf
    public static String SWAP_SPF_SWAP = "swap";
    public static String SWAP_SPF_SWAP_SWAPSIZE = "swap_size";
    public static String SWAP_SPF_SWAP_FIRST = "swap_first";
    public static String SWAP_SPF_ZRAM = "zram";
    public static String SWAP_SPF_ZRAM_SIZE = "zram_size";
    public static String SWAP_SPF_SWAPPINESS = "swappiness";

    public static String XPOSED_DPI_SPF = "xposed_dpi"; //spf
    public static String XPOSED_HIDETASK_SPF = "xposed_hidetask"; //spf
}
