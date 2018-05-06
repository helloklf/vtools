#!/system/bin/sh

setprop persist.camera.HAL3.enabled $1;
echo '已修改，可能需要重启才能生效！';