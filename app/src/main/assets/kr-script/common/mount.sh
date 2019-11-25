#!/system/bin/sh

# 挂载系统分区为读写
function mount_all() {
    $BUSYBOX mount -o rw,remount / 2> /dev/null
    mount -o rw,remount / 2> /dev/null

    $BUSYBOX mount -o rw,remount /system 2> /dev/null
    mount -o rw,remount /system 2> /dev/null

    $BUSYBOX mount -o remount,rw /dev/block/bootdevice/by-name/system /system 2> /dev/null
    mount -o remount,rw /dev/block/bootdevice/by-name/system /system 2> /dev/null

    $BUSYBOX mount -o rw,remount /vendor 2> /dev/null
    mount -o rw,remount /vendor 2> /dev/null

    $BUSYBOX mount -o rw,remount /system/vendor 2> /dev/null
    mount -o rw,remount /system/vendor 2> /dev/null

    if [[ -e /dev/block/bootdevice/by-name/vendor ]]; then
        $BUSYBOX mount -o rw,remount /dev/block/bootdevice/by-name/vendor /vendor 2> /dev/null
        mount -o rw,remount /dev/block/bootdevice/by-name/vendor /vendor 2> /dev/null
    fi
}
