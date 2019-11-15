#!/system/bin/sh

path="/system/etc/hosts"
if [ -n `cat $path | grep "update.miui.com"` ]; then
    echo 1;
else
    echo 0;
fi;
