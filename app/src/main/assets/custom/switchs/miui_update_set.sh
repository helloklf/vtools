#!/system/bin/sh

echo '屏蔽MIUI在线更新下载地址(需要解锁System分区)...'

$BUSYBOX mount -o rw,remount /system
mount -o rw,remount /system
$BUSYBOX mount -o remount,rw /dev/block/bootdevice/by-name/system /system
mount -o remount,rw /dev/block/bootdevice/by-name/system /system 2> /dev/null

path="/system/etc/hosts"
$BUSYBOX sed '/127.0.0.1\ \ \ \ \ \ \ update.miui.com/'d $path > /cache/hosts

if [[ ! $state = 1 ]]; then
    $BUSYBOX sed -i '$a127.0.0.1\ \ \ \ \ \ \ update.miui.com' /cache/hosts
    pm clear com.android.updater 2> /dev/null
    echo '已添加“127.0.0.1        update.miui.com”到hosts'
fi;

cp /cache/hosts $path
chmod 0755 $path
rm /cache/hosts
sync

echo '可能需要重启手机才会生效！'
