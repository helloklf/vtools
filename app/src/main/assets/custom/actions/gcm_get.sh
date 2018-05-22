#!/system/bin/sh

gcm=`getprop persist.camera.HAL3.enabled`
text='启用Camera2 API，从而支持谷歌相机HDR+功能或避免闪退（谷歌相机需要自行下载适合自己机型的版本）。可能导致相机无法开启或切换缓慢，当前状态：'

if [ "$gcm" = '1' ]; then
    echo $text '已启用'
else
    echo $text '未启用'
fi