#!/system/bin/sh

source ./kr-script/common/props.sh

prop="miui.usb.dialog"

magisk_set_system_prop $prop $state

if [[ "$?" = "1" ]];
then
    echo "已通过Magisk更改 $prop，需要重启才能生效！"
else
    set_system_prop $prop $state
fi

