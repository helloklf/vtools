package com.omarea.store;

/**
 * 公共参数
 * Created by helloklf on 2017/11/02.
 */

public class SpfConfig {
    public static String POWER_CONFIG_SPF = "powercfg";
    public static String HOME_QUICK_SWITCH = "home_quick_switch";

    public static String CHARGE_SPF = "charge"; //spf
    public static String CHARGE_SPF_QC_BOOSTER = "qc_booster"; //bool
    public static String CHARGE_SPF_QC_LIMIT = "charge_limit_ma"; //int
    public static int CHARGE_SPF_QC_LIMIT_DEFAULT = 3000; //int
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
    // 执行模式
    public static String CHARGE_SPF_EXEC_MODE = "";
    public static int CHARGE_SPF_EXEC_MODE_SPEED_UP = 0; // 目标 加快充电
    public static int CHARGE_SPF_EXEC_MODE_SPEED_DOWN = 1; // 目标 降低速度保护电池
    public static int CHARGE_SPF_EXEC_MODE_SPEED_FORCE = 2; // 目标 强制加速
    public static int CHARGE_SPF_EXEC_MODE_DEFAULT = CHARGE_SPF_EXEC_MODE_SPEED_UP; // 目标（默认设置）

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
    public static String GLOBAL_SPF_HELP_ICON = "show_help_icon";
    public static String GLOBAL_SPF_SKIP_AD = "is_skip_ad";
    public static String GLOBAL_SPF_SKIP_AD_PRECISE = "is_skip_ad_precise2";
    public static String GLOBAL_SPF_DISABLE_ENFORCE = "enforce_0";
    public static String GLOBAL_SPF_START_DELAY = "start_delay";
    public static String GLOBAL_SPF_SCENE_LOG = "scene_logview";
    public static String GLOBAL_SPF_AUTO_EXIT = "auto_exit";
    public static String GLOBAL_SPF_NIGHT_MODE = "app_night_mode";
    public static String GLOBAL_SPF_THEME = "app_theme2";
    public static String GLOBAL_SPF_MAC = "wifi_mac";
    public static String GLOBAL_SPF_MAC_AUTOCHANGE_MODE = "wifi_mac_autochange_mode";
    public static int GLOBAL_SPF_MAC_AUTOCHANGE_MODE_1 = 1;
    public static int GLOBAL_SPF_MAC_AUTOCHANGE_MODE_2 = 2;
    public static String GLOBAL_SPF_POWERCFG_FIRST_MODE = "powercfg_first_mode";
    public static String GLOBAL_SPF_POWERCFG_SLEEP_MODE = "powercfg_sleep_mode";
    public static String GLOBAL_SPF_DYNAMIC_CONTROL = "dynamic_control";
    public static boolean GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT = false;
    public static String GLOBAL_SPF_DYNAMIC_CONTROL_STRICT = "dynamic_control_strict";
    public static String GLOBAL_SPF_DYNAMIC_CONTROL_DELAY = "dynamic_control_delay";
    public static String GLOBAL_SPF_PROFILE_SOURCE = "scene4_profile_source";
    public static String GLOBAL_SPF_POWERCFG = "global_powercfg";
    public static String GLOBAL_SPF_CONTRACT = "global_contract_scene4";
    public static String GLOBAL_SPF_POWERCFG_FRIST_NOTIFY = "global_powercfg_notifyed";
    public static String GLOBAL_SPF_LAST_UPDATE = "global_last_update";
    public static String GLOBAL_SPF_CURRENT_NOW_UNIT = "global_current_now_unit";
    public static int GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT = -1000;
    public static String GLOBAL_SPF_FREEZE_ICON_NOTIFY = "freeze_icon_notify";
    public static String GLOBAL_SPF_FREEZE_SUSPEND = "freeze_suspend";
    public static String GLOBAL_SPF_FREEZE_DELAY = "freeze_screen_off_delay"; // 息屏后处理延迟
    public static String GLOBAL_SPF_FREEZE_TIME_LIMIT = "freeze_suspend_time_limit";
    public static String GLOBAL_SPF_FREEZE_ITEM_LIMIT = "freeze_suspend_item_limit";
    public static String GLOBAL_SPF_FREEZE_XPOSED_OPEN = "freeze_xposed_open";
    public static String GLOBAL_SPF_FREEZE_CLICK_OPEN = "freeze_click_open";
    public static String GLOBAL_NIGHT_BLACK_NOTIFICATION = "night_black_notification";

    public static String SWAP_SPF = "swap"; //spf
    public static String SWAP_SPF_SWAP = "swap";
    public static String SWAP_SPF_SWAP_SWAPSIZE = "swap_size";
    public static String SWAP_SPF_SWAP_PRIORITY = "swap_priority";
    public static String SWAP_SPF_SWAP_USE_LOOP = "swap_use_loop";
    public static String SWAP_SPF_ZRAM = "zram";
    public static String SWAP_SPF_ZRAM_SIZE = "zram_size";
    public static String SWAP_SPF_SWAPPINESS = "swappiness";
    public static String SWAP_SPF_EXTRA_FREE_KBYTES = "extra_free_kbytes";
    public static String SWAP_SPF_WATERMARK_SCALE = "watermark_scale";
    public static String SWAP_SPF_AUTO_LMK = "auto_lmk";
    public static String SWAP_SPF_ALGORITHM = "comp_algorithm"; // zram 压缩算法

    public static String SCENE_BLACK_LIST = "scene_black_list_spf";
    public static String AUTO_SKIP_BLACKLIST = "AUTO_SKIP_BLACKLIST";
}
