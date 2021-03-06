file="/system/etc/hosts"

if [[ "$MAGISK_PATH" != "" ]]; then
    # Magisk 替换

    if [[ ! -f $MAGISK_PATH$file ]]; then
        mkdir -p $MAGISK_PATH/system/etc
        cp $file $MAGISK_PATH$file
    fi

    if [[ $state == 1 ]]; then
        # 恢复更新 移除规则
        $BUSYBOX sed -i '/127.0.0.1\ \ \ \ \ \ \ update.miui.com/'d $MAGISK_PATH$file
    else
        # 屏蔽更新 添加规则
        $BUSYBOX sed -i '$a127.0.0.1\ \ \ \ \ \ \ update.miui.com' $MAGISK_PATH$file
    fi
    pm clear com.android.updater 2> /dev/null

    echo '此操作需要重启手机方可生效！'
else
    # 非 Magisk 替换

    source ./kr-script/common/mount.sh
    mount_all

    echo '屏蔽MIUI在线更新下载地址(需要解锁System分区)...'

    path="/system/etc/hosts"
    $BUSYBOX sed '/127.0.0.1\ \ \ \ \ \ \ update.miui.com/'d $path > /cache/hosts

    if [[ ! $state = 1 ]]; then
        $BUSYBOX sed -i '$a127.0.0.1\ \ \ \ \ \ \ update.miui.com' /cache/hosts
        pm clear com.android.updater 2> /dev/null
        echo '已添加“127.0.0.1        update.miui.com”到hosts'
    fi;

    cp /cache/hosts $path
    chmod 0755 $path
    rm /cache/hosts
    sync

    echo '可能需要重启手机才会生效！'
fi

