#!/system/bin/sh

function set_value()
{
    if [[ -f "$1" ]];
    then
        chmod 0666 "$1"
        echo "$2" > "$1"
    fi
}

max='/sys/class/power_supply/battery/constant_charge_current_max'
bce='/sys/class/power_supply/battery/battery_charging_enabled'
suspend='/sys/class/power_supply/battery/input_suspend'

if [[ -f $bce ]]
then
    set_value $bce 0
    setprop vtools.bp 1
elif [[ -f $suspend ]]
then
    set_value $suspend 1
    setprop vtools.bp 1
elif [[ -f $max ]]
then
    set_value $max 0
    setprop vtools.bp 1
else
    echo 'error'
fi
