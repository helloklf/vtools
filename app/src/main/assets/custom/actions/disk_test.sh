#!/system/bin/sh

echo '测试需要2G以上的可用存储空间'
echo '建议把CPU开到最高性能再测啦！'
echo '同步写入会使缓存增益失效，更能反映存储芯片的的真实性能！'
echo ''

rm -f /data/testtmp 2> /dev/null
echo '\n常规写入测试...'
busybox dd if=/dev/zero of=/data/testtmp bs=1024000 count=1024

rm -f /data/testtmp 2> /dev/null
sync
echo 3 > /proc/sys/vm/drop_caches

echo '\n同步写入测试...'
busybox dd if=/dev/zero of=/data/testtmp bs=1024000 count=1024 conv=fsync
sync
echo 3 > /proc/sys/vm/drop_caches

echo '\n读取速度测试...'
busybox dd if=/data/testtmp of=/dev/null

echo ''
echo '回收缓存...'
rm -f /data/testtmp 2> /dev/null
sync
echo 3 > /proc/sys/vm/drop_caches
echo ''