#!/system/bin/sh

echo '删除耗电记录'
echo '注意：只是清空耗电曲线，并不会恢复电池寿命！！！'
rm -f /data/system/batterystats-checkin.bin 2>/dev/null
rm -f /data/system/batterystats-daily.xml 2>/dev/null
rm -f /data/system/batterystats.bin 2>/dev/null
rm -rf /data/system/battery-history 2>/dev/null
rm -rf /data/vendor/charge_logger 2>/dev/null
rm -rf /data/charge_logger 2>/dev/null

echo '即将重启设备'
sync
sleep 2
reboot


