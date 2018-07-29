#!/system/bin/sh

#[dalvik.vm.boot-dex2oat-threads]: [8]
#[dalvik.vm.dex2oat-threads]: [4]
#[dalvik.vm.image-dex2oat-threads]: [4]
#[ro.sys.fw.dex2oat_thread_count]: [4]


echo 'Step1.挂载System为读写...'

$BUSYBOX mount -o rw,remount /system
mount -o rw,remount /system
$BUSYBOX mount -o remount,rw /dev/block/bootdevice/by-name/system /system
mount -o remount,rw /dev/block/bootdevice/by-name/system /system 2> /dev/null

echo 'Step2.移除已有配置'
$BUSYBOX sed '/dalvik.vm.boot-dex2oat-threads=.*/'d /system/build.prop > "/data/build.prop"
$BUSYBOX sed -i '/dalvik.vm.dex2oat-threads=.*/'d "/data/build.prop"
$BUSYBOX sed -i '/dalvik.vm.image-dex2oat-threads=.*/'d "/data/build.prop"
$BUSYBOX sed -i '/ro.sys.fw.dex2oat_thread_count=.*/'d "/data/build.prop"


echo 'Step2.更新配置'
if [[ -n $boot ]]; then
    $BUSYBOX sed -i "\$adalvik.vm.boot-dex2oat-threads=$boot" /data/build.prop;
    $BUSYBOX sed -i "\$aro.sys.fw.dex2oat_thread_count=$boot" /data/build.prop;
    setprop dalvik.vm.boot-dex2oat-threads $boot
    setprop ro.sys.fw.dex2oat_thread_count $boot
fi;

if [[ -n $dex2oat ]]; then
    $BUSYBOX sed -i "\$adalvik.vm.dex2oat-threads=$dex2oat" /data/build.prop;
    setprop dalvik.vm.dex2oat-threads $dex2oat
fi;

if [[ -n $image ]]; then
    $BUSYBOX sed -i "\$adalvik.vm.image-dex2oat-threads=$image" /data/build.prop;
    setprop dalvik.vm.image-dex2oat-threads $image
fi;

echo 'Step3.写入文件'
if [[ ! -e "/system/build.prop.bak" ]]; then
    cp /system/build.prop /system/build.prop.bak
fi;
cp /data/build.prop /system/build.prop
chmod 0644 /system/build.prop
rm /data/build.prop
chmod 0644 /system/build.prop.bak


echo '操作完成...'
echo '现在，请重启手机使修改生效！'
