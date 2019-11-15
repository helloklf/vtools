#!/system/bin/sh

echo '关闭网络'
settings put global airplane_mode_on 1;
am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true 2> /dev/null

if [[ "$state" = 1 ]]
then
    settings put global captive_portal_server https://connect.rom.miui.com/generate_204
    settings put global captive_portal_http_url https://connect.rom.miui.com/generate_204
    settings put global captive_portal_https_url https://connect.rom.miui.com/generate_204
    settings put global captive_portal_use_https 1
    settings put global captive_portal_mode 1
    settings put global captive_portal_detection_enabled 1
    echo '已将网络校验服务器更改为 https://connect.rom.miui.com/generate_204'
else
    settings delete global captive_portal_server
    settings reset global captive_portal_server
    settings delete global captive_portal_http_url
    settings reset global captive_portal_http_url
    settings delete global captive_portal_https_url
    settings reset global captive_portal_https_url
    settings delete global captive_portal_use_https
    settings reset global captive_portal_use_https
    settings delete global captive_portal_mode
    settings reset global captive_portal_mode
    settings delete global captive_portal_detection_enabled
    settings reset global captive_portal_detection_enabled
    echo '已重置网络校验参数'
fi

echo '重启网络'
settings put global airplane_mode_on 0;
am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false 2> /dev/null
