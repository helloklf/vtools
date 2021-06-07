source ./kr-script/common/mount.sh
mount_all

echo '注意：本功能只对骁龙820及同代处理器的设备旧版系统有较好的效果！'
echo '但是，这可能会导致相机打开缓慢，相机无法正常启动等问题'
echo '而在新设备上使用，则基本不会产生良好的效益'

files="/system/vendor/bin/thermal-engine /system/vendor/lib64/libthermalclient.so /system/vendor/lib64/libthermalioctl.so /system/vendor/lib/libthermalclient.so"

if [[ $state = '0' ]]; then
    echo '备份并删除'
    for file in $files; do
        echo $file

        cp $file "${file}.bak"
        rm -f $file
    done
else
    echo '还原'
    for file in $files; do
        echo $file

        cp "${file}.bak" $file
        chmod 755 $file
    done
fi

echo '操作完成，请重启手机后再次检查开关状态！'
