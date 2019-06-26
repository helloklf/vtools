#!/system/bin/sh

# 限制电流值 3000
limit_value=$1
if [[ "$limit_value" = "" ]]
then
    limit_value=3000
fi

paths=`ls /sys/class/power_supply/*/constant_charge_current_max`

# 更改限制 change_limit ?mA
function change_limit() {
    echo "更改限制值为：${1}mA"
    local limit="${1}000"

    for path in $paths
    do
        chmod 0664 $path
        echo $limit > $path
    done

    if [[ -f /sys/class/qcom-battery/restricted_current ]]; then
        chmod 0664 /sys/class/qcom-battery/restricted_current
        echo $limit > /sys/class/qcom-battery/restricted_current
    fi
}

if [[ `getprop vtools.fastcharge` = "" ]]; then
    ./fast_charge_run_once.sh

    setprop vtools.fastcharge 1
fi

change_limit $limit_value
