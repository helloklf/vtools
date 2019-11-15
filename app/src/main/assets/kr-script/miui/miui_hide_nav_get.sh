#!/system/bin/sh

source ./kr-script/common/magisk_replace.sh

file_mixture_hooked "./kr-script/miui/resources/framework-res" "/system/media/theme/default/framework-res"
result="$?"

echo "$result"
