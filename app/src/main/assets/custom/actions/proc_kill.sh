#!/system/bin/sh

stop woodpeckerd 2> /dev/null
stop debuggerd 2> /dev/null
stop debuggerd64 2> /dev/null
stop atfwd 2> /dev/null
stop perfd 2> /dev/null
stop logd 2> /dev/null
if [[ -e /sys/zte_power_debug/switch ]]; then
    echo 0 > /sys/zte_power_debug/switch
fi
if [[ -e /sys/zte_power_debug/debug_enabled ]]; then
    echo N > /sys/kernel/debug/debug_enabled
fi
stop cnss_diag 2> /dev/null
stop subsystem_ramdump 2> /dev/null
#stop thermal-engine 2> /dev/null
stop tcpdump 2> /dev/null
stop logd 2> /dev/null
stop adbd 2> /dev/null

echo '执行完成，立即生效！'

echo '注意：重启手机后此操作会失效！'