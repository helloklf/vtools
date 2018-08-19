#!/system/bin/sh

echo '本测试会禁用系统读写入缓存，因此和AndroBench测试结果会有差异'
echo '闪存品质、处理器性能都将影响测试结果！'

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