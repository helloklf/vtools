dir=/system/usr/keylayout
file=$dir/gpio-keys.kl

if [[ -f $MAGISK_PATH$file ]]; then
    full_path=$MAGISK_PATH$file
else
    full_path=$file
fi

grep '^key 377' $full_path | awk -F ' ' '{print $3}'
