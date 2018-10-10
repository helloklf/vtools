#!/system/bin/sh

function set_rw()
{
    if [[ -f "$1" ]];
    then
        chmod 0666 "$1"
    fi
}

function set_value()
{
    if [[ -f "$1" ]];
    then
        chmod 0666 "$1"
        echo "$2" > "$1"
    fi
}

set_value /sys/class/power_supply/usb/pd_allowed 1
set_rw /sys/class/power_supply/main/constant_charge_current_max
set_rw /sys/class/qcom-battery/restricted_current
set_value /sys/class/qcom-battery/restricted_charging 0
set_value /sys/class/power_supply/battery/restricted_charging 0
set_value /sys/class/power_supply/battery/safety_timer_enabled 0
set_value /sys/class/power_supply/bms/temp_warm 50
set_rw /sys/class/power_supply/battery/constant_charge_current_max

