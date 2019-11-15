#!/system/bin/sh

mode=`settings get global low_power`
if [[ "$mode" = "1" ]];
then
    echo 1;
else
    echo 0;
fi

