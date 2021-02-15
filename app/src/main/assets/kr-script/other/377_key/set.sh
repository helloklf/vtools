dir=/system/usr/keylayout
file=$dir/gpio-keys.kl

if [[ "$state" != "" ]] && [[ "$state" != "INFO" ]]; then
    if [[ "$MAGISK_PATH" != "" ]]; then
        if [[ ! -f $MAGISK_PATH$file ]]; then
            mkdir -p $MAGISK_PATH$dir
            cp $file $MAGISK_PATH$file
        fi
        # busybox sed -i "s/^原内容/替换为/" 文件路径

        busybox sed -i "s/^key 377.*/key 377   $state/" $MAGISK_PATH$file
        echo $state
        echo '此修改，需要重启手机才能生效！' 1>&2
    else
        echo '未安装附加模块，无法应用修改~' 1>&2
    fi
else
    if [[ -f $MAGISK_PATH$file ]]; then
        rm $MAGISK_PATH$file
        echo '此修改，需要重启手机才能生效！' 1>&2
    fi
fi