#!/system/bin/sh

set_value() {
  if [[ -f "$1" ]];
  then
    chmod 0666 "$1"
    echo "$2" > "$1"
  fi
}


max='/sys/class/power_supply/battery/constant_charge_current_max'
bce='/sys/class/power_supply/battery/battery_charging_enabled'
suspend='/sys/class/power_supply/battery/input_suspend'
suspend2='/sys/class/qcom-battery/input_suspend'

if [[ -f $max ]] && [[ `cat $max` = "0" ]]; then
  set_value $max 3000000
  current_max_path=$max
  # "/sys/class/power_supply/battery/constant_charge_current_max"
  current_max_backup="vtools.charge.current.max"
  current_max=`getprop $current_max_backup`
  if [[ ! "$current_max" == "" ]] ; then
    set_value $current_max_path $current_max
  fi
fi

if [[ -f $bce ]] || [[ -f $suspend ]] || [[ -f $suspend2 ]]; then
  set_value $bce 1
  set_value $suspend 0
  set_value $suspend2 0
  setprop vtools.bp 0
else
  echo 'error'
fi
