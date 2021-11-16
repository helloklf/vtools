#!/system/bin/sh

# 限制电流值 3000
limit_value=$1
# 是否只往上加
only_taller=$2

if [[ "$limit_value" = "" ]]
then
    limit_value=3000
fi

device=$(getprop ro.product.device)
# Xiaomi 11Pro/Ultra
if [[ "$device" == "mars" ]] || [[ "$device" == "star" ]]; then
  echo "${1}000" > /sys/class/power_supply/battery/constant_charge_current
  return
fi

paths=`ls /sys/class/power_supply/*/constant_charge_current_max`

# 更改限制 change_limit ?mA
change_limit() {
    echo "更改限制值为：${1}mA"
    local limit="${1}000"

    for path in $paths
    do
        chmod 0664 $path
        if [[ "$only_taller" = 1 ]]; then
            local current_limit=`cat $path`
            if [[ "$current_limit" -lt "$limit" ]]; then
                echo $limit > $path
            fi
        else
            echo $limit > $path
        fi
    done
}

if [[ `getprop vtools.fastcharge` = "" ]]; then
  ./fast_charge_run_once.sh
  setprop vtools.fastcharge 1
fi

change_limit $limit_value
# echo `date "+%Y-%m-%d %H:%M:%S.%MS"` " -> $limit_value" >> /cache/scene_charge.log
