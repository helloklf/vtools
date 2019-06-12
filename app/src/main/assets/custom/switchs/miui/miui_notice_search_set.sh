#!/system/bin/sh

resource=./custom/switchs/resources/com.android.systemui
output=/system/media/theme/default/com.android.systemui

source ./custom/common/magisk_replace.sh

if [[ "$state" = 1 ]]
then
    mixture_hook_file "$resource" "$output" 0
else
    mixture_hook_file "$resource" "$output" 1
fi