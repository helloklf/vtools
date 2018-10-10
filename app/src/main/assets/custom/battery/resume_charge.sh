#!/system/bin/sh

if [ -f /sys/class/power_supply/battery/constant_charge_current_max ]
then
    echo 3000000 > /sys/class/power_supply/battery/constant_charge_current_max
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
