#!/system/bin/sh

source ./kr-script/common/magisk_replace.sh

file_mixture_hooked "./kr-script/switchs/resources/com.android.systemui" "/system/media/theme/default/com.android.systemui"
result="$?"

if [[ "$result" = "1" ]]
then
    echo 0
else
    echo 1
fi
