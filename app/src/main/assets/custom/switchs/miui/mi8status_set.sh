#!/system/bin/sh

source ./custom/common/props.sh

prop="ro.miui.notch"

magisk_set_system_prop $prop $state

if [[ "$?" = "1" ]];
then
    echo "已通过Magisk更改 $prop，需要重启才能生效！"
else
    set_system_prop $prop $state
fi
