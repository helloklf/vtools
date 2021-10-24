#!/system/bin/sh

echo '关闭网络'
settings put global airplane_mode_on 1 > /dev/null 2>&1
am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true > /dev/null 2>&1

if [[ "$state" == "" ]] || [[ "$state" == "default" ]]; then
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
elif [[ "$state" == "disable" ]]; then
    settings put global captive_portal_mode 0
    echo '已禁用网络状态监测'
    echo '注意：这将导致你在连接到公共WIFI时，不会自动弹出登录验证！' 1>&2
else
    server_url=""
    if [[ "$state" == "miui" ]]; then
      server_url="//connect.rom.miui.com/generate_204"
    elif [[ "$state" == "google" ]]; then
      server_url="//www.google.cn/generate_204"
    elif [[ "$state" == "v2ex" ]]; then
      server_url="//captive.v2ex.co/generate_204"
    fi
    settings put global captive_portal_server https:$server_url
    settings put global captive_portal_http_url http:$server_url
    settings put global captive_portal_https_url https:$server_url
    settings put global captive_portal_use_https 1
    settings put global captive_portal_mode 1
    settings put global captive_portal_detection_enabled 1
    echo "已将网络检测服务器更改为 https:$server_url"
fi

echo '重启网络'
settings put global airplane_mode_on 0 > /dev/null 2>&1
am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false > /dev/null 2>&1
