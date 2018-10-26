#!/system/bin/sh

gcm=`settings get global enable_freeform_support`

if [ "$gcm" = '1' ]; then
    echo 1
else
    echo 0
fi