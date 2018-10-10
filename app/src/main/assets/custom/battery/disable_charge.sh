#!/system/bin/sh

if [ -f /sys/class/power_supply/battery/battery_charging_enabled ]
then
    echo 0 > /sys/class/power_supply/battery/battery_charging_enabled
    setprop vtools.bp 1
elif [ -f /sys/class/power_supply/battery/constant_charge_current_max ]
then
    chmod 664 /sys/class/power_supply/battery/constant_charge_current_max
    echo 0 > /sys/class/power_supply/battery/constant_charge_current_max
    setprop vtools.bp 1
elif [[ -f /sys/class/power_supply/battery/input_suspend ]]
then
    echo 1 > /sys/class/power_supply/battery/input_suspend
    setprop vtools.bp 1
else
    echo 'error'
fi
