level_low=100
# level_high=10000
# capacity_max=80
# charging_time=6
pause_time=1.5

main_constant=/sys/class/power_supply/main/constant_charge_current_max
bms_constant=/sys/class/power_supply/bms/constant_charge_current_max
battery_constant=/sys/class/power_supply/battery/constant_charge_current_max
step=/sys/class/power_supply/battery/step_charging_enabled

chmod 664 $step
echo 0 > $step
chmod 664 $battery_constant
chmod 664 $bms_constant
chmod 664 $main_constant

if [[ -f $battery_constant ]]; then
    test_value=${level_high}000
    echo $test_value > $battery_constant
    result=`cat $battery_constant`
    if [[ ! "$result" == "$test_value" ]]; then
        echo '输入的数值超出系统限制范围！' 1>&2
        echo '输入限制数值：'${level_high} 1>&2
        echo '实际生效数值：'$((result/1000)) 1>&2
        echo '这可能导致模拟脉冲无效！'
        # exit 1
    fi
fi

echo '理论上，模拟脉冲充电能明显减少发热'
echo '但由于无法突破硬件极限限制，模拟脉冲充电后'
echo '充电整体速度会有所下降'
echo ''
echo '当前设置为'
echo "以${level_high}mA上限速度充电${charging_time}s"
echo "后暂停${pause_time}s进行循环，直至"
echo "电池电量大于${capacity_max}%结束模拟"
echo ''

echo '开始运行……'
while [[ 1 = 1 ]]
do
    capacity=$(cat /sys/class/power_supply/battery/capacity)
    if  [[ $capacity -gt $capacity_max ]]; then
        echo "电量 > ${capacity_max}%，已停止运行"
        echo "progress:[$capacity/$capacity]"
        break
    fi

    echo "progress:[$capacity/100]"
    # echo limit $level_low
    echo ${level_low}000 > $main_constant
    echo ${level_low}000 > $bms_constant
    echo ${level_low}000 > $battery_constant
    sleep $pause_time

    echo "progress:[-1/100]"
    # echo resume $level_high
    echo ${level_high}000 > $main_constant
    echo ${level_high}000 > $battery_constant
    sleep $charging_time
done
