#!/system/bin/sh

cache=/data/testtmp

echo '测试需要2G以上的可用存储空间'
echo '建议把CPU开到最高性能再测啦！'
echo '同步写入会使缓存增益失效，更能反映存储芯片的的真实性能！'
echo ''

rm -f $cache 2> /dev/null
echo '\n常规写入测试...'
sync
echo 3 > /proc/sys/vm/drop_caches
sleep 3
$BUSYBOX dd if=/dev/zero of=$cache bs=1024000 count=2048 conv=sync

rm -f $cache 2> /dev/null
sync
echo 3 > /proc/sys/vm/drop_caches

echo '\n同步写入测试...'
sleep 3
$BUSYBOX dd if=/dev/zero of=$cache bs=1024000 count=2048 conv=fsync
sync
echo 3 > /proc/sys/vm/drop_caches

if [[ -e /dev/block/sda ]];
then
    echo '\n缓存速度测试...'
    sleep 3
    $BUSYBOX hdparm -T /dev/block/sda

    echo '\n读取测试...'
    sleep 3
    $BUSYBOX hdparm -t /dev/block/sda
else
    echo '\n常规读取测试...'
    sleep 3
    $BUSYBOX dd if=$cache of=/dev/null
fi

echo ''
echo '回收缓存...'
rm -f $cache 2> /dev/null
sync
echo 3 > /proc/sys/vm/drop_caches
echo ''