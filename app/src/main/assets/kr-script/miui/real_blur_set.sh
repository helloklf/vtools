source ./kr-script/common/props.sh

prop="ro.miui.has_real_blur"

magisk_set_system_prop $prop $state

if [[ "$?" = "1" ]];
then
    echo "已通过Magisk更改 $prop，需要重启才能生效！"
else
    set_system_prop $prop $state
fi
