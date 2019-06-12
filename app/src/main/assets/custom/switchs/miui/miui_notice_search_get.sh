#!/system/bin/sh

source ./custom/common/magisk_replace.sh

file_mixture_hooked "./custom/switchs/resources/com.android.systemui" "/system/media/theme/default/com.android.systemui"
result="$?"

if [[ "$result" = "1" ]]
then
    echo 0
else
    echo 1
fi
