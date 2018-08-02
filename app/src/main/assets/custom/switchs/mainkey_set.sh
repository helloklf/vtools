#!/system/bin/sh
state=$1

echo '使用本功能，需要解锁system分区，否则修改无效！'
echo 'MIUI自带的ROOT无法使用本功能'

echo '1.挂载/system为读写(可能会报错，问题不大)'

$BUSYBOX mount -o rw,remount /system
mount -o rw,remount /system
$BUSYBOX mount -o remount,rw /dev/block/bootdevice/by-name/system /system
mount -o remount,rw /dev/block/bootdevice/by-name/system /system 2> /dev/null

$BUSYBOX sed '/qemu.hw.mainkeys=/'d /system/build.prop > /cache/build.prop
if [ $state == 1 ];then
    $BUSYBOX sed -i '$aqemu.hw.mainkeys=0' /cache/build.prop
    echo '2.修改qemu.hw.mainkeys=0'
else
    $BUSYBOX sed -i '$aqemu.hw.mainkeys=1' /cache/build.prop
    echo '2.修改qemu.hw.mainkeys=1'
fi
echo '3.覆盖/system/build.prop'
cp /cache/build.prop /system/build.prop

echo '4.修正读写权限'
chmod 0644 /system/build.prop

echo '5.删除临时文件'
rm /cache/build.prop
sync

echo ''
echo '重启后生效！'
