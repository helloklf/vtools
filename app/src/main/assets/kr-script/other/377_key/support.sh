377_key=`grep '^key 377' /system/usr/keylayout/gpio-keys.kl`
if [[ "$377_key" == "" ]]; then
    echo 0
else
    echo 1
fi
