#!/system/bin/sh

if [ `grep ro\.miui\.notch=1 /system/build.prop|cut -d '=' -f2` = 1 ]; then
    echo 1;
else
    echo 0;
fi;
