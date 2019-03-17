#!/system/bin/sh

state="$1"
if [ "$1" = '1' ];then
    state="true"
else
    state="false"
fi
prop="ro.config.low_ram"

function magisk_set_system_prop() {
    if [[ -d "$MAGISK_PATH" ]];
    then
        echo "你已安装Magisk，本次修改将通过操作进行"
        $BUSYBOX sed -i "/$1=/"d "$MAGISK_PATH/system.prop"
        $BUSYBOX sed -i "\$a$1=$2" "$MAGISK_PATH/system.prop"
        setprop $1 $2 2> /dev/null
        return 1
    fi;
    return 0
}

magisk_set_system_prop $prop $state

if [[ "$?" = "1" ]];
then
    echo "已通过Magisk更改 $prop，需要重启才能生效！"
else
    path="/system/build.prop"
    if [[ -f /vendor/build.prop ]] && [[ -n `cat /vendor/build.prop | grep ro\.config\.low_ram=` ]]
    then
        path="/vendor/build.prop"
    fi

    echo '使用本功能，需要解锁system分区，否则修改无效！'
    echo 'MIUI自带的ROOT无法使用本功能'

    echo 'Step1.挂载/system为读写'
    $BUSYBOX mount -o rw,remount /system 2> /dev/null
    mount -o rw,remount /system 2> /dev/null
    $BUSYBOX mount -o remount,rw /dev/block/bootdevice/by-name/system /system 2> /dev/null
    mount -o remount,rw /dev/block/bootdevice/by-name/system /system 2> /dev/null

    busybox mount -o rw,remount /vendor 2> /dev/null
    mount -o rw,remount /vendor 2> /dev/null

    $BUSYBOX sed "/$prop=/"d $path > /cache/build.prop
    $BUSYBOX sed -i "\$a$prop=$state" /cache/build.prop
    echo "Step2.修改$prop=$state"

    echo 'Step3.写入文件'
    cp /cache/build.prop $path
    chmod 0755 $path

    echo 'Step4.删除临时文件'
    rm /cache/build.prop
    sync

    echo ''
    echo '重启后生效！'
    echo '部分系统开启以后可能无法进入桌面，或出现卡顿！！！'
fi
