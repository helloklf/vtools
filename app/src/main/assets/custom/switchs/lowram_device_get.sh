#!/system/bin/sh

if [ `grep ro\.config\.low_ram= /system/build.prop|cut -d '=' -f2` = "true" ]; then
    echo 1;
elif [ -f '/vendor/build.prop' ] && [ `grep ro\.config\.low_ram= /vendor/build.prop|cut -d '=' -f2` = "true" ]; then
    echo 1;
else
    echo 0;
fi;
