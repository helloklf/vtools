#!/system/bin/sh

function unlock_dir() {
    chattr -i $1 2> /dev/null
    $BUSYBOX chattr -i $1 2> /dev/null
    rm -rf $1 2> /dev/null
}

function lock_dir() {
    unlock_dir $1
    echo "" > $1
    chattr +i $1 2> /dev/null
    $BUSYBOX chattr +i $1 2> /dev/null
}


if [[ "$state" = "0" ]]; then
    lock_dir "$SDCARD_PATH/netease/cloudmusic/Ad"
    lock_dir "$SDCARD_PATH/Android/data/com.netease.cloudmusic/cache/Ad"
else
    unlock_dir "$SDCARD_PATH/netease/cloudmusic/Ad"
    unlock_dir "$SDCARD_PATH/Android/data/com.netease.cloudmusic/cache/Ad"
fi
