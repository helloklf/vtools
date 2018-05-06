#!/system/bin/sh

gcm=`getprop persist.camera.HAL3.enabled`
text='在/system/build.prop中添加persist.camera.HAL3.enabled=1，从而使用谷歌相机DHR+功能。可能导致相机无法开启或切换缓慢，当前状态：'

if [ $gcm = '1' ]; then
    echo $text '已添加'
else
    echo $text '未添加'
fi