#!/system/bin/sh

model=`getprop ro.product.device`
for config in `ls "/system/etc/device_features/$model.xml"`
do
    round=`cat $config | grep "<bool name=\"support_round_corner\">true</bool>"`
    if [ -n "$round" ]; then
        echo 1;
    else
        echo 0;
    fi;
    return;
done
