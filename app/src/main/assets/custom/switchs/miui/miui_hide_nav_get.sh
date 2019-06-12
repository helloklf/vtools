#!/system/bin/sh

source ./custom/common/magisk_replace.sh

file_mixture_hooked "./custom/switchs/resources/framework-res" "/system/media/theme/default/framework-res"
result="$?"

echo "$result"
