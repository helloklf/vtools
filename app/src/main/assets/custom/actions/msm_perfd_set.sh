#!/system/bin/sh

path=/system/vendor/bin/perfd

if [[ ! -f $path ]]; then
    if [[ ! -f $path.bak ]]; then
        echo '当前设备或系统不支持此操作！'
        exit 0;
    fi;
fi;

echo 'Step1.挂载System、Vendor为读写...'

$BUSYBOX mount -o rw,remount /system
mount -o rw,remount /system
$BUSYBOX mount -o remount,rw /dev/block/bootdevice/by-name/system /system
mount -o remount,rw /dev/block/bootdevice/by-name/system /system 2> /dev/null

$BUSYBOX mount -o rw,remount /vendor 2> /dev/null
mount -o rw,remount /vendor 2> /dev/null
$BUSYBOX mount -o rw,remount /system/vendor 2> /dev/null
mount -o rw,remount /system/vendor 2> /dev/null

if [[ "$config" = "1" ]]; then
    if [[ -f "$path.bak" ]]; then
        mv "$path.bak" "$path"
        chmod 0644 "$path"
        echo "Step2.还原（${path}.bak）->（$path） ..."
    fi;
elif [[ "$config" = "0" ]]; then
    if [[ -f "$path" ]]; then
        mv "$path" "$path.bak"
        chmod 0644 "$path.bak"
        echo "Step2.重命名 （$path） ->（${path}.bak） ..."
    fi;
fi;

echo '操作完成...'
echo '现在，请重启手机使修改生效！'
