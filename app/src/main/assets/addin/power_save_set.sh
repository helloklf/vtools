#!/system/bin/sh
state=$1
settings put global low_power $1;
settings put global low_power_sticky $1;

function killproc()
{
    stop "$1" 2> /dev/null
    killall -9 "$1" 2> /dev/null
}

# Whether or not app auto restriction is enabled. When it is enabled, settings app will  auto restrict the app if it has bad behavior(e.g. hold wakelock for long time).
# [app_auto_restriction_enabled]

#Whether or not to enable Forced App Standby on small battery devices.         * Type: int (0 for false, 1 for true)
# forced_app_standby_for_small_battery_enabled

# Feature flag to enable or disable the Forced App Standby feature.         * Type: int (0 for false, 1 for true)
# forced_app_standby_enabled

# Whether or not to enable the User Absent, Radios Off feature on small battery devices.         * Type: int (0 for false, 1 for true)
# user_absent_radios_off_for_small_battery_enabled

echo '充电状态下可能无法使用省电模式'
echo '-'

if [[ $state = "1" ]]
then
    echo "开启应用自动限制 可能需要Android Pie"
    settings put global app_auto_restriction_enabled true

    echo "开启应用强制standby"
    settings put global forced_app_standby_enabled 1

    echo "开启应用standby"
    settings put global app_standby_enabled 1

    echo "开启小容量电池设备应用强制standby"
    settings put global forced_app_standby_for_small_battery_enabled 1

    ai=`settings get system ai_preload_user_state`
    if [[ ! "$ai" = "null" ]]
    then
      echo "关闭MIUI10的ai预加载"
      settings put system ai_preload_user_state 0
    fi

    echo "开启安卓原生的省电模式"
    settings put global low_power 1
    settings put global low_power_sticky 1

    echo "关闭调试服务和日志进程"
    killproc cnss_diag
    killproc subsystem_ramdump
    killproc tcpdump
    # killproc logd
    # killproc adbd
    #stop thermal-engine 2> /dev/null
    #killall -9 magiskd 2> /dev/null
    if [[ -e /sys/zte_power_debug/switch ]]; then
        echo 0 > /sys/zte_power_debug/switch
    fi
    if [[ -e /sys/zte_power_debug/debug_enabled ]]; then
        echo N > /sys/kernel/debug/debug_enabled
    fi
    #killall -9 magiskd 2> /dev/null
    killall -9 magisklogd 2> /dev/null

    echo "清理后台休眠白名单"
    echo "请稍等..."
    for item in `dumpsys deviceidle whitelist`
    do
        app=`echo "$item" | cut -f2 -d ','`
        #echo "deviceidle whitelist -$app"
        dumpsys deviceidle whitelist -$app 2>&1 >/dev/null
        am set-inactive $app true 2>&1 >/dev/null
        am set-idle $app true 2>&1 >/dev/null
        # 9.0 让后台应用立即进入闲置状态
        am make-uid-idle --user current $app 2>&1 >/dev/null
    done
    for app in `pm list packages -3  | cut -f2 -d ':'`
    do
        am set-inactive $app true > /dev/null 2>&1
        am set-idle $app true > /dev/null 2>&1
        am make-uid-idle --user current $app > /dev/null 2>&1
    done
    dumpsys deviceidle step
    dumpsys deviceidle step
    dumpsys deviceidle step
    dumpsys deviceidle step

    echo 3 > /proc/sys/vm/drop_caches

    echo '注意：开启省电模式后，Scene可能会无法保持后台'
    echo '并且，可能会收不到后台消息推送！'
    echo ''
else
    echo "关闭应用自动限制 可能需要Android Pie"
    settings put global app_auto_restriction_enabled false
    settings reset global app_auto_restriction_enabled

    echo "关闭应用强制standby"
    settings put global forced_app_standby_enabled 0
    settings reset global forced_app_standby_enabled

    echo "开启应用standby"
    settings put global app_standby_enabled 1
    settings reset global app_standby_enabled

    echo "关闭小容量电池设备应用强制standby"
    settings put global forced_app_standby_for_small_battery_enabled 0
    settings reset global forced_app_standby_for_small_battery_enabled

    echo "关闭安卓原生的省电模式"
    settings put global low_power 0
    settings put global low_power_sticky 0
    settings reset global low_power
    settings reset global low_power_sticky
fi

echo '状态已切换，部分深度定制的系统此操作可能无效！'
echo '-'


