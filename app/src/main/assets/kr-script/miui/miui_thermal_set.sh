#!/system/bin/sh

am broadcast --user 0 -a update_profile com.miui.powerkeeper/com.miui.powerkeeper.cloudcontrol.CloudUpdateReceiver

services=`settings get secure enabled_accessibility_services`
service='com.qualcomm.qti.perfdump/com.qualcomm.qti.perfdump.AutoDetectService'
include=$(echo "$services" | grep "$service")

if [ ! -n "$services" ]
then
    settings put secure enabled_accessibility_services "$service";
elif [ ! -n "$include" ]
then
    settings put secure enabled_accessibility_services "$services:$service"
else
    settings put secure enabled_accessibility_services "$services"
fi
settings put secure accessibility_enabled 1;

echo '如果没有报错（红色字体），那么现在去“设置 - 电池和性能”看看，是不是已经有温控模式选项了吧！'

sleep 1
