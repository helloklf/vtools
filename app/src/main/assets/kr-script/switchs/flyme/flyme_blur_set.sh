#!/system/bin/sh

source ./kr-script/common/props.sh

prop="persist.sys.static_blur_mode"

if [[ $state = 1 ]]
then
    value=false
else
    value=true
fi

magisk_set_system_prop $prop $value

if [[ "$?" = "1" ]];
then
    echo "已通过Magisk更改 $prop，需要重启才能生效！"
else
    set_system_prop $prop $value
fi
