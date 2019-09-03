package com.omarea.store;

/**
 * 公共参数
 * Created by helloklf on 2017/11/02.
 */

public class SpfConfig {
    public static String POWER_CONFIG_SPF = "powercfg";

    public static String CHARGE_SPF = "charge"; //spf
    public static String CHARGE_SPF_QC_BOOSTER = "qc_booster"; //bool
    public static String CHARGE_SPF_QC_LIMIT = "charge_limit_ma"; //int
    public static String CHARGE_SPF_BP = "bp"; //bool
    public static String CHARGE_SPF_BP_LEVEL = "bp_level"; //int
    public static int CHARGE_SPF_BP_LEVEL_DEFAULT = 90; //int
    // 是否开启睡眠时间充电速度调整
    public static String CHARGE_SPF_NIGHT_MODE = "sleep_time"; //bool
    // 起床时间
    public static String CHARGE_SPF_TIME_GET_UP = "time_get_up"; //int（hours*60 + minutes）
    // 起床时间（默认为7:00）
    public static int CHARGE_SPF_TIME_GET_UP_DEFAULT = 7 * 60; //
    // 睡觉时间
    public static String CHARGE_SPF_TIME_SLEEP = "time_slepp"; //int（hours*60 + minutes）
    // 睡觉时间（默认为22:30点）
    public static int CHARGE_SPF_TIME_SLEEP_DEFAULT = 22 * 60 + 30;

    public static String BOOSTER_SPF_CFG_SPF = "boostercfg2";
    public static String DATA = "data";
    public static String WIFI = "wifi";
    public static String NFC = "nfc";
    public static String GPS = "gps";
    public static String FORCEDOZE = "doze";
    public static String POWERSAVE = "powersave";

    public static String ON = "_on";
    public static String OFF = "_off";

    public static String GLOBAL_SPF = "global"; //spf
    public static String GLOBAL_SPF_AUTO_INSTALL = "is_auto_install";
    public static String GLOBAL_SPF_DISABLE_ENFORCE = "enforce_0";
    public static String GLOBAL_SPF_AUTO_STARTED_COMPILE = "auto_started_compile";
    public static String GLOBAL_SPF_AUTO_STARTED_FSTRIM = "auto_started_fstrim";
    public static String GLOBAL_SPF_START_DELAY = "start_delay";
    public static String GLOBAL_SPF_DELAY = "is_delay_start";
    public static String GLOBAL_SPF_NOTIFY = "scene_notify";
    public static String GLOBAL_SPF_AUTO_REMOVE_RECENT = "remove_recent";
    public static String GLOBAL_SPF_NIGHT_MODE = "app_night_mode";
    public static String GLOBAL_SPF_THEME = "app_theme";
    public static String GLOBAL_SPF_MAC = "wifi_mac";
    public static String GLOBAL_SPF_MAC_AUTOCHANGE = "wifi_mac_autochange";
    public static String GLOBAL_SPF_BATTERY_MONITORY = "power_config_battery_monitor";
    public static String GLOBAL_SPF_POWERCFG_FIRST_MODE = "powercfg_first_mode";
    public static String GLOBAL_SPF_DYNAMIC_CONTROL = "dynamic_control";
    public static String GLOBAL_SPF_LOCK_MODE = "lock_mode";
    public static String GLOBAL_SPF_POWERCFG = "global_powercfg";
    public static String GLOBAL_SPF_CONTRACT = "global_contract";
    public static String GLOBAL_SPF_POWERCFG_FRIST_NOTIFY = "global_powercfg_notifyed";
    public static String GLOBAL_SPF_LAST_UPDATE = "global_last_update";
    public static String GLOBAL_SPF_CURRENT_NOW_UNIT = "global_current_now_unit";
    public static int GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT = -1000;
    public static String GLOBAL_SPF_BUSYBOX_INSTALLED = "busybox_installed";

    public static String SWAP_SPF = "swap"; //spf
    public static String SWAP_SPF_SWAP = "swap";
    public static String SWAP_SPF_SWAP_SWAPSIZE = "swap_size";
    public static String SWAP_SPF_SWAP_FIRST = "swap_first";
    public static String SWAP_SPF_SWAP_USE_LOOP = "swap_use_loop";
    public static String SWAP_SPF_ZRAM = "zram";
    public static String SWAP_SPF_ZRAM_SIZE = "zram_size";
    public static String SWAP_SPF_SWAPPINESS = "swappiness";
    public static String SWAP_SPF_AUTO_LMK = "auto_lmk";
    public static String SWAP_SPF_ALGORITHM = "comp_algorithm"; // zram 压缩算法

    public static String KEY_EVENT_SPF = "key_event_spf";
    public static String KEY_EVENT_ONTHER_CONFIG_SPF = "key_event_spf2";
    public static String CONFIG_SPF_TOUCH_BAR = "touch_bar";

    public static String APP_HIDE_HISTORY_SPF = "app_hide_spf";
}
