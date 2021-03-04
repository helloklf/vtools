file="/system/etc/hosts"

if [[ -f $MAGISK_PATH$file ]]; then
    full_path=$MAGISK_PATH$file
else
    full_path=$file
fi

if [[ -n `cat $full_path | grep "update.miui.com" | grep "127.0.0.1"` ]]; then
    echo 0;
else
    echo 1;
fi;