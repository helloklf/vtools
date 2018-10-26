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


if [ -f /sys/class/power_supply/battery/constant_charge_current_max ]
then
    if [ `/sys/class/power_supply/battery/battery_charging_enabled` = '0' ]
    then
        set_value > /sys/class/power_supply/battery/battery_charging_enabled 3000000
    fi
fi;

#"if [ -f /sys/class/power_supply/main/constant_charge_current_max ]; then echo 3000000 > /sys/class/power_supply/main/constant_charge_current_max;fi;\n" +
#"if [ -f /sys/class/qcom-battery/restricted_current ]; then echo 3000000 > /sys/class/qcom-battery/restricted_current;fi;\n" +

if [ -f '/sys/class/power_supply/battery/battery_charging_enabled' ]
then
    echo 1 > /sys/class/power_supply/battery/battery_charging_enabled;
else
    echo 0 > /sys/class/power_supply/battery/input_suspend;
fi;

setprop vtools.bp 0;
