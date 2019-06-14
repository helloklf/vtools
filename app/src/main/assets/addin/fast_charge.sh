#!/system/bin/sh

# 限制电流值 3000
limit_value="$1"
if [[ "$limit_value" = "" ]]
then
    echo '未输入电流(mA)参数，将使用默认值：3000'
    echo '如需使用自定义值，请带参运行脚本'
    echo '如：./fast_charge.sh 3500'
    limit_value="3000"
fi

# 工具函数
function set_rw()
{
    if [[ -f "$1" ]];
    then
        chmod 0664 "$1"
    fi
}

# 工具函数
function set_value()
{
    if [[ -f "$1" ]];
    then
        chmod 0664 "$1"
        echo "$2" > "$1"
    fi
}

# 工具函数（跳过是否存在检测）
function set_value_sc()
{
    chmod 0664 "$1"
    echo "$2" > "$1"
}

# 只运行一次 修改一些额外的参数权限
function run_once() {
    if [[ ! -f /dev/fast_charge_scene ]]
    then
        set_value /sys/class/qcom-battery/restricted_charging 0
        set_value /sys/class/power_supply/battery/restricted_charging 0
        set_value /sys/class/power_supply/usb/pd_allowed 1
        set_value /sys/class/power_supply/battery/subsystem/usb/pd_allowed 1
        set_value /sys/class/power_supply/battery/safety_timer_enabled 0
        set_value /sys/class/power_supply/bms/temp_warm 500
        set_rw /sys/class/power_supply/main/constant_charge_current_max
        set_rw /sys/class/power_supply/battery/constant_charge_current_max
        set_rw /sys/class/qcom-battery/restricted_current

        # 部分设备不支持超过3000的值，所以首次执行先设为3000吧
        change_limit 3000

        touch /dev/fast_charge_scene
    fi
}

paths=`ls /sys/class/power_supply/*/constant_charge_current_max`

# 更改限制 change_limit ?mA
function change_limit() {
    echo "更改限制值为：${1}mA"
    local limit="${1}000"

    for path in $paths
    do
        set_value_sc $path $limit
    done

    set_value /sys/class/qcom-battery/restricted_current "$limit"
}

# 阶梯修改限制值
function ladder_change_limit() {
    local current=3000
    local limit=$1

    while [ $current -lt $limit ]
    do
        change_limit $current
        # let current=current+300
        # current=$(($current + 300))
        local current=`expr $current + 300`
        if [[ "$current" = "" ]] || [[ "$current" = 3000 ]] || [[ "$current" = "$1" ]]
        then
            break
        fi
        echo $limit
    done
    change_limit $limit
}

run_once

if [[ $limit_value -gt 3000 ]]
then
    ladder_change_limit $limit_value
else
    change_limit $limit_value
fi
