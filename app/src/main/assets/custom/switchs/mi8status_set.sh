#!/system/bin/sh
state=$1

echo '使用本功能，需要解锁system分区，否则修改无效！'
echo 'MIUI自带的ROOT无法使用本功能'

echo '1.挂载/system为读写(可能会报错，问题不大)'

$BUSYBOX mount -o rw,remount /system
mount -o rw,remount /system
$BUSYBOX mount -o remount,rw /dev/block/bootdevice/by-name/system /system
mount -o remount,rw /dev/block/bootdevice/by-name/system /system 2> /dev/null

busybox mount -o rw,remount /vendor 2> /dev/null
mount -o rw,remount /vendor 2> /dev/null

path="/system/build.prop"
if [[ -f /vendor/build.prop ]] && [[ -n `cat /vendor/build.prop | grep ro\.miui\.notch=` ]]
then
    path="/vendor/build.prop"
fi

$BUSYBOX sed '/ro.miui.notch=/'d $path > /cache/build.prop
if [ $state == 1 ];then
    $BUSYBOX sed -i '$aro.miui.notch=1' /cache/build.prop
    echo '2.修改ro.miui.notch=1'
else
    $BUSYBOX sed -i '$aro.miui.notch=0' /cache/build.prop
    echo '2.修改ro.miui.notch=0'
fi
echo '3.覆盖/system/build.prop'
cp /cache/build.prop $path

echo '4.修正读写权限'
chmod 0755 $path

echo '5.删除临时文件'
rm /cache/build.prop
sync

echo ''
echo '重启后生效！'
