#!/system/bin/sh

echo '使用本功能，需要解锁system分区，否则修改无效！'
echo 'MIUI自带的ROOT无法使用本功能'

echo '挂载/system为读写'

$BUSYBOX mount -o rw,remount /system
mount -o rw,remount /system
$BUSYBOX mount -o remount,rw /dev/block/bootdevice/by-name/system /system
mount -o remount,rw /dev/block/bootdevice/by-name/system /system 2> /dev/null

model=`getprop ro.product.device`
for config in `ls "/system/etc/device_features/$model.xml"`
do
    if [ ! -f "$config.bak" ]; then
        echo '备份文件...'
        cp $config $config.bak
    fi;

    echo '修改文件...'
    $BUSYBOX sed -i '/.*<!--whether round corner-->/'d "$config"
    $BUSYBOX sed -i '/.*<bool name="support_round_corner">.*<\/bool>/'d "$config"

    if [[ $state = 1 ]]; then
        $BUSYBOX sed -i '2a \ \ \ \ <!--whether round corner-->' $config
        $BUSYBOX sed -i '3a \ \ \ \ <bool name="support_round_corner">true<\/bool>' $config
    else
        $BUSYBOX sed -i '2a \ \ \ \ <!--whether round corner-->' $config
        $BUSYBOX sed -i '3a \ \ \ \ <bool name="support_round_corner">false<\/bool>' $config
    fi;

    sync
    chmod 755 $config

    echo '操作完成，请重启手机！'
    return
done
