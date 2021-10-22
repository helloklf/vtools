#!/system/bin/sh

source ./kr-script/common/magisk_replace.sh

t_media="/system/media/theme/default"

mixture_hook_file "./kr-script/miui/resources/framework-res" "$t_media/framework-res" "$state"
mixture_hook_file "./kr-script/miui/resources/com.miui.home" "$t_media/com.miui.home" "$state"

wm overscan reset 2>/dev/null
