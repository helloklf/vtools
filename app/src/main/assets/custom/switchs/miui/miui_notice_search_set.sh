#!/system/bin/sh

resource=./custom/switchs/resources/com.android.systemui
output=/system/media/theme/default/com.android.systemui

if [[ -f $resource ]]
then
    echo '使用本功能，需要解锁system分区，否则修改无效！'
    echo 'MIUI自带的ROOT无法使用本功能'

    echo '挂载/system为读写'

    if [[ $state = 0 ]]
    then
        $BUSYBOX mount -o rw,remount /system
        mount -o rw,remount /system
        $BUSYBOX mount -o remount,rw /dev/block/bootdevice/by-name/system /system
        mount -o remount,rw /dev/block/bootdevice/by-name/system /system 2> /dev/null

        cp $resource $output
        chmod 0755 $output
    else
        rm -f $output
    fi

    echo '需要重启才能生效~'
else
    echo '所需的资源文件缺失，无法进行操作' 1>&2
fi
