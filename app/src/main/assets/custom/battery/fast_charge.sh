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
        chmod 0666 "$1"
    fi
}

# 工具函数
function set_value()
{
    if [[ -f "$1" ]];
    then
        chmod 0666 "$1"
        echo "$2" > "$1"
    fi
}

# 只运行一次 修改一些额外的参数权限
function run_once() {
    if [[ ! -f /dev/fast_charge_scene ]]
    then
        # set_value /sys/class/power_supply/usb/pd_allowed 1
        # set_value '1' /sys/class/power_supply/battery/subsystem/usb/pd_allowed
        set_rw /sys/class/power_supply/main/constant_charge_current_max
        set_rw /sys/class/qcom-battery/restricted_current
        #set_value /sys/class/qcom-battery/restricted_charging 0
        #set_value /sys/class/power_supply/battery/restricted_charging 0
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
    set_value /sys/class/power_supply/battery/input_current_limited '0'
    set_value /sys/class/power_supply/dc/current_max "$limit"
    set_value /sys/class/power_supply/main/current_max "$limit"
    set_value /sys/class/power_supply/parallel/current_max "$limit"
    set_value /sys/class/power_supply/pc_port/current_max "$limit"
    set_value /sys/class/power_supply/usb/current_max "$limit"
    set_value /sys/class/power_supply/usb/hw_current_max "$limit"
    set_value /sys/class/power_supply/usb/pd_current_max "$limit"
    set_value /sys/class/power_supply/usb/ctm_current_max "$limit"
    set_value /sys/class/power_supply/usb/sdp_current_max "$limit"
    set_value /sys/class/power_supply/main/constant_charge_current_max "$limit"
    set_value /sys/class/power_supply/parallel/constant_charge_current_max "$limit"
    set_value /sys/class/power_supply/battery/constant_charge_current_max "$limit"
}

#
# function main() {
#     # 电源状态 2:充电，其它:非充电
#     local status=`dumpsys battery | grep "status:" | cut -f2 -d "\:" | cut -f2 -d " "`
#     # 电池电量 0 - 100
#     local level=`dumpsys battery | grep "level:" | cut -f2 -d "\:" | cut -f2 -d " "`
#
#     if [[ "$level" -gt "90" ]]
#     then
#         echo '电量大于90%'
#         change_limit 100
#     else
#         echo '电量小于90%'
#         change_limit 3000
#     fi
# }

run_once
change_limit $limit_value

# while [ 1 = 1 ]
# do
#     main
#     sleep 5
# done
