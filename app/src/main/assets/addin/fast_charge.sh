#!/system/bin/sh

# 限制电流值 3000
limit_value=$1
# 是否只往上加
only_taller=$2

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
        if [[ "$only_taller" = 1 ]]; then
            local current_limit=`cat $path`
            if [[ "$current_limit" -lt "$limit" ]]; then
                echo $limit > $path
            fi
        else
            echo $limit > $path
        fi
    done

    restricted="/sys/class/qcom-battery/restricted_current"
    if [[ -f $restricted ]]; then
        chmod 0664 $restricted
        if [[ "$only_taller" = 1 ]]; then
            local current_limit=`cat $restricted`
            if [[ "$current_limit" -lt "$limit" ]]; then
                echo $limit > $restricted
            fi
        else
            echo $limit > $restricted
        fi
    fi
}

if [[ `getprop vtools.fastcharge` = "" ]]; then
    ./fast_charge_run_once.sh

    setprop vtools.fastcharge 1
fi

change_limit $limit_value
