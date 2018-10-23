#!/system/bin/sh

echo '删除耗电记录'
echo '注意：只是清空耗电曲线，并不会恢复电池寿命！！！'
rm -f /data/system/batterystats-checkin.bin
rm -f /data/system/batterystats-daily.xml
rm -f /data/system/batterystats.bin

echo '即将重启设备'
sync
sleep 5
reboot


