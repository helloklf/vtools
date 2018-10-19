#!/system/bin/sh
state=$1
settings put global low_power $1;
settings put global low_power_sticky $1;

# Whether or not app auto restriction is enabled. When it is enabled, settings app will  auto restrict the app if it has bad behavior(e.g. hold wakelock for long time).
# [app_auto_restriction_enabled]

#Whether or not to enable Forced App Standby on small battery devices.         * Type: int (0 for false, 1 for true)
# forced_app_standby_for_small_battery_enabled

# Feature flag to enable or disable the Forced App Standby feature.         * Type: int (0 for false, 1 for true)
# forced_app_standby_enabled

# Whether or not to enable the User Absent, Radios Off feature on small battery devices.         * Type: int (0 for false, 1 for true)
# user_absent_radios_off_for_small_battery_enabled

level=`settings get global low_power_trigger_level`
maxlevel=`settings get global low_power_trigger_level_max`
if [[ "$level" = "null" ]]
then
    echo ''
    #echo '进入省电模式的电流级别当前设为null，将不会自动进入省电模式'
fi

echo '充电状态下无法使用省电模式'
echo '-'

echo '状态已切换，部分深度定制的系统此操作可能无效！'
echo '-'

echo '注意：开启省电模式后，Scene可能会无法保持后台'
echo ''


