#!/system/bin/sh

rm -f /sdcard/tmp 2> /dev/null
sync
echo 3 > /proc/sys/vm/drop_caches
echo '测试写入速度...'
busybox dd if=/dev/zero of=/sdcard/tmp bs=1024000 count=1024 conv=fsync
sync
echo 3 > /proc/sys/vm/drop_caches
echo '测试读取速度...'
busybox dd if=/sdcard/tmp of=/dev/null
echo '清理垃圾文件...'
rm -f /sdcard/tmp 2> /dev/null
sync
echo 3 > /proc/sys/vm/drop_caches