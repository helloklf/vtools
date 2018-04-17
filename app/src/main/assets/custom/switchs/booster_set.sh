#!/system/bin/sh
state=$1

echo $state > /proc/sys/kernel/sched_boost


sleep 1;

if [ $state = `cat /proc/sys/kernel/sched_boost` ]; then
    if [ `cat /proc/sys/kernel/sched_boost` = '1' ]; then
        echo '切换成功，当前状态：大核优先';
    else
        echo '切换成功，当前状态：小核优先';
    fi;
else
    echo '切换失败！'
fi;
