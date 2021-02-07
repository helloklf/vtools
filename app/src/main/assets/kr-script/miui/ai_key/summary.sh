dir=/system/usr/keylayout
file=$dir/gpio-keys.kl

if [[ -f $MAGISK_PATH$file ]]; then
    full_path=$MAGISK_PATH$file
else
    full_path=$file
fi

key_code=`grep '^key 689' $full_path | awk -F ' ' '{print $3}'`

if [[ "$key_code" != "" ]];then
    key_name=`grep "$key_code" $PAGE_WORK_DIR/ai_key/options.txt | awk -F '|' '{print $2}'`
    if [[ "$key_name" == "" ]]; then
        echo $key_code
    else
        echo $key_name
    fi
fi
