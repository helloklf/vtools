#!/system/bin/sh

#[dalvik.vm.boot-dex2oat-threads]: [8]
#[dalvik.vm.dex2oat-threads]: [4]
#[dalvik.vm.image-dex2oat-threads]: [4]
#[ro.sys.fw.dex2oat_thread_count]: [4]

filepath=""
if [[ -n "$MAGISK_PATH" ]];
then
    filepath="${MAGISK_PATH}/system.prop"
    echo "你已安装Magisk，本次修改将通过操作进行"
    echo 'Step1.挂载System为读写（跳过）...'
else
    filepath="/system/build.prop"

    echo 'Step1.挂载System为读写...'

    source ./kr-script/common/mount.sh
    mount_all

    if [[ ! -e "/system/build.prop.bak" ]]; then
        cp /system/build.prop /system/build.prop.bak
        chmod 0755 /system/build.prop.bak
    fi;
fi;

echo 'Step2.移除已有配置'
$BUSYBOX sed '/dalvik.vm.boot-dex2oat-threads=.*/'d $filepath > "/data/build.prop"
$BUSYBOX sed -i '/dalvik.vm.dex2oat-threads=.*/'d "/data/build.prop"
$BUSYBOX sed -i '/dalvik.vm.image-dex2oat-threads=.*/'d "/data/build.prop"
$BUSYBOX sed -i '/ro.sys.fw.dex2oat_thread_count=.*/'d "/data/build.prop"


echo 'Step2.更新配置'
if [[ -n $boot ]]; then
    $BUSYBOX sed -i "\$adalvik.vm.boot-dex2oat-threads=$boot" /data/build.prop;
    $BUSYBOX sed -i "\$aro.sys.fw.dex2oat_thread_count=$boot" /data/build.prop;
    setprop dalvik.vm.boot-dex2oat-threads $boot 2> /dev/null
    setprop ro.sys.fw.dex2oat_thread_count $boot 2> /dev/null
fi;

if [[ -n $dex2oat ]]; then
    $BUSYBOX sed -i "\$adalvik.vm.dex2oat-threads=$dex2oat" /data/build.prop;
    setprop dalvik.vm.dex2oat-threads $dex2oat 2> /dev/null
fi;

if [[ -n $image ]]; then
    $BUSYBOX sed -i "\$adalvik.vm.image-dex2oat-threads=$image" /data/build.prop;
    setprop dalvik.vm.image-dex2oat-threads $image 2> /dev/null
fi;

echo 'Step3.写入文件'
cp /data/build.prop $filepath
chmod 0755 $filepath
rm /data/build.prop

echo '操作完成...'
echo '现在，请重启手机使修改生效！'
