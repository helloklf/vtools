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

if [ -f /sys/class/power_supply/battery/battery_charging_enabled ]
then
    if [ ! `/sys/class/power_supply/battery/battery_charging_enabled` = '0' ]
    then
        set_value > /sys/class/power_supply/battery/battery_charging_enabled 0
        setprop vtools.bp 1
    fi
elif [[ -f /sys/class/power_supply/battery/input_suspend ]]
then
    echo 1 > /sys/class/power_supply/battery/input_suspend
    setprop vtools.bp 1
elif [ -f /sys/class/power_supply/battery/constant_charge_current_max ]
then
    chmod 664 /sys/class/power_supply/battery/constant_charge_current_max
    echo 0 > /sys/class/power_supply/battery/constant_charge_current_max
    setprop vtools.bp 1
else
    echo 'error'
fi
