#!/system/bin/sh

# 限制电流值 3000
limit_value="$1"
if [[ "$limit_value" = "" ]]
then
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

# 只运行一次 修改一些额外的参数权限
function run_once() {
    if [[ ! -f /dev/fast_charge_scene ]]
    then
        set_value /sys/class/power_supply/usb/pd_allowed 1
        set_value '1' /sys/class/power_supply/battery/subsystem/usb/pd_allowed
        set_rw /sys/class/power_supply/main/constant_charge_current_max
        set_rw /sys/class/qcom-battery/restricted_current
        set_value /sys/class/power_supply/battery/safety_timer_enabled 0
        set_value /sys/class/power_supply/bms/temp_warm 500
        set_rw /sys/class/power_supply/battery/constant_charge_current_max

        # 部分设备不支持超过3000的值，所以首次执行先设为3000吧
        change_limit 3000

        touch /dev/fast_charge_scene
    fi
}

# 更改限制 change_limit ?mA
function change_limit() {
    echo "更改限制值为：${1}mA"
    local limit="${1}000"

    for path in /sys/class/power_supply/*/constant_charge_current_max
    do
      set_value "$path" "$limit"
    done
}

run_once
change_limit $limit_value