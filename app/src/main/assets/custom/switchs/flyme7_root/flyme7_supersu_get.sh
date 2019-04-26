#!/system/bin/sh

if [ -f "/system/xbin/daemonsu" ]
then
    echo 1
else
    echo 0
fi
