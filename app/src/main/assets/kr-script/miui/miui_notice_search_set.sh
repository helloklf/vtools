#!/system/bin/sh

resource=./kr-script/miui/resources/com.android.systemui
output=/system/media/theme/default/com.android.systemui

source ./kr-script/common/magisk_replace.sh

if [[ "$state" = 1 ]]
then
    mixture_hook_file "$resource" "$output" 0
else
    mixture_hook_file "$resource" "$output" 1
fi