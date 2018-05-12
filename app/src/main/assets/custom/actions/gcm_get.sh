#!/system/bin/sh

gcm=`getprop persist.camera.HAL3.enabled`
text='添加persist.camera.HAL3.enabled=1，从而使用谷歌相机HDR+功能或避免闪退（谷歌相机需要自行下载适合自己机型的版本）。可能导致相机无法开启或切换缓慢，当前状态：'

if [ $gcm = '1' ]; then
    echo $text '已添加'
else
    echo $text '未添加'
fi