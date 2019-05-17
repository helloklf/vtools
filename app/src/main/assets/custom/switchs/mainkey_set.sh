#!/system/bin/sh

source ./custom/common/props.sh

if [[ $state = 1 ]]
then
    value=0
else
    value=1
fi
prop="qemu.hw.mainkeys"

magisk_set_system_prop $prop $value

if [[ "$?" = "1" ]];
then
    echo "已通过Magisk更改 $prop，需要重启才能生效！"
else
    set_system_prop $prop $value
fi
