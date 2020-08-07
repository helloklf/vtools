#! /vendor/bin/sh

MODDIR=${0%/*}

setprop sys.lmk.minfree_levels "12800:0,16384:100,18432:200,20480:250,24576:900,32768:950"

echo 0 > /proc/sys/vm/swappiness

echo '' > /cache/lmkd_opt.log

# 某些参数修改执行的太早会被系统覆盖掉，所以延迟30秒

sh /system/bin/memory_config.sh >> /cache/lmkd_opt.log 2>&1

sleep 15

setprop sys.lmk.minfree_levels "12800:0,16384:100,18432:200,20480:250,24576:900,32768:950"
echo "12800,16384,18432,20480,24576,32768" > /sys/module/lowmemorykiller/parameters/minfree
echo 0 > /sys/module/lowmemorykiller/parameters/enable_lmk
echo 0 > /sys/module/lowmemorykiller/parameters/enable_adaptive_lmk

# sh /system/bin/memory_config.sh >> /cache/lmkd_opt.log 2>&1

stop lmkd
# 
start lmkd

echo "compact" >> /cache/lmkd_opt.log

sleep 10
sh /system/bin/force_compact.sh >> /cache/lmkd_opt.log 2>&1

echo 100 > /proc/sys/vm/swappiness

busybox=/data/adb/magisk/busybox
if [[ -f $busybox ]]; then
  $busybox fstrim /data 2>/dev/null
  $busybox fstrim /cache 2>/dev/null
  $busybox fstrim /system 2>/dev/null
  $busybox fstrim /data 2>/dev/null
  $busybox fstrim /cache 2>/dev/null
  $busybox fstrim /system 2>/dev/null
fi

echo "completed！" >> /cache/lmkd_opt.log
