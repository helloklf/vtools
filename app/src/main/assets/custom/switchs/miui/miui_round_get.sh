#!/system/bin/sh

for config in `ls /system/etc/device_features/*.xml`
do
    round=`cat $config | grep "<bool name=\"support_round_corner\">true</bool>"`
    if [ -n "$round" ]; then
        echo 1;
    else
        echo 0;
    fi;
    return;
done
