#!/system/bin/sh

if [ `grep qemu\.hw\.mainkeys= /system/build.prop|cut -d '=' -f2` = 1 ]; then
    echo 0;
elif [ -f '/vendor/build.prop' ] && [ `grep qemu\.hw\.mainkeys= /vendor/build.prop|cut -d '=' -f2` = 1 ]; then
    echo 0;
else
    echo 1;
fi;
