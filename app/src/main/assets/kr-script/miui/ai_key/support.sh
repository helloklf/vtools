ai_key=`grep '^key 689' /system/usr/keylayout/gpio-keys.kl`
if [[ "$ai_key" == "" ]]; then
    echo 0
else
    echo 1
fi
