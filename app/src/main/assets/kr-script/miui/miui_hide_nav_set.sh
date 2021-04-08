#!/system/bin/sh

source ./kr-script/common/magisk_replace.sh

mixture_hook_file "./kr-script/miui/resources/framework-res" "/system/media/theme/default/framework-res" "$state"
wm overscan reset 2>/dev/null
