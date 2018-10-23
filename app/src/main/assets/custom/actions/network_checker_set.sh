#!/system/bin/sh

settings put global airplane_mode_on 1;
am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true;
if [[ "$1" = 1 ]]
then
    settings put global captive_portal_server https://connect.rom.miui.com/generate_204
    settings put global captive_portal_http_url https://connect.rom.miui.com/generate_204
    settings put global captive_portal_https_url https://connect.rom.miui.com/generate_204
    settings put global captive_portal_use_https 1
    settings put global captive_portal_mode 1
    settings put global captive_portal_detection_enabled 1
else
    settings reset global captive_portal_server
    settings reset global captive_portal_http_url
    settings reset global captive_portal_https_url
    settings reset global captive_portal_use_https
    settings reset global captive_portal_mode
    settings reset global captive_portal_detection_enabled
fi
settings put global airplane_mode_on 0;
am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false;