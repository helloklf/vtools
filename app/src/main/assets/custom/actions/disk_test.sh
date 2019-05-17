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
echo "progress:[-1/5]"

$BUSYBOX dd if=/dev/zero of=$cache bs=1024000 count=2048 conv=sync 1>&2
rm -f $cache 2> /dev/null
sync
echo 3 > /proc/sys/vm/drop_caches
echo "progress:[1/5]"

echo '\n同步写入测试...'
$BUSYBOX dd if=/dev/zero of=$cache bs=1024000 count=2048 conv=fsync 1>&2
sync
echo 3 > /proc/sys/vm/drop_caches
echo "progress:[2/5]"

if [[ -e /dev/block/sda ]];
then
    echo '\n缓存速度测试...'
    $BUSYBOX hdparm -T /dev/block/sda 1>&2
    echo "progress:[3/5]"

    echo '\n读取测试...'
    $BUSYBOX hdparm -t /dev/block/sda 1>&2
    echo "progress:[4/5]"
else
    echo '\n常规读取测试...'
    $BUSYBOX dd if=$cache of=/dev/null 1>&2
    echo "progress:[4/5]"
fi

echo ''
echo '回收缓存...'
rm -f $cache 2> /dev/null
sync
echo 3 > /proc/sys/vm/drop_caches
echo ''
echo "progress:[5/5]"