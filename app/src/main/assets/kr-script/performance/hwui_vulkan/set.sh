#!/system/bin/sh

source ./kr-script/common/props.sh

if [[ $state = 1 ]]
then
    value='true'
else
    value='false'
fi

prop="ro.hwui.use_vulkan"

magisk_set_system_prop $prop $value

if [[ "$?" = "1" ]];
then
    echo "已通过Magisk更改 $prop，需要重启才能生效！"
else
    set_system_prop $prop $value
fi

