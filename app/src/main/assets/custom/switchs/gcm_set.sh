#!/system/bin/sh

gcm=$1

setprop persist.camera.HAL3.enabled $gcm;
echo '已修改，可能需要重启才能生效！';
result=`getprop persist.camera.HAL3.enabled`
echo "persist.camera.HAL3.enabled=${result}"
