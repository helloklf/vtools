#!/system/bin/sh

if [ `grep ro\.miui\.has_security_keyboard= /system/build.prop|cut -d '=' -f2` = 1 ]; then
    echo 1;
elif [ -f '/vendor/build.prop' ] && [ `grep ro\.miui\.has_security_keyboard= /vendor/build.prop|cut -d '=' -f2` = 1 ]; then
    echo 1;
else
    echo 0;
fi;
