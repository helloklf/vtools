#!/system/bin/sh

if [[ ! -n "$SDCARD_PATH" ]]; then
    if [[ -e /storage/emulated/0/Android ]]; then
        SDCARD_PATH="/storage/emulated/0"
    elif [[ -e /sdcard/Android ]]; then
        SDCARD_PATH="/sdcard/"
    elif [[ -e /data/media/0/Android ]]; then
        SDCARD_PATH="/data/media/0"
    fi
fi

path=$SDCARD_PATH/netease/cloudmusic/Ad

if [[ ! -e $path ]]; then
    echo 1;
    return
fi

if [[ -d $path ]]; then
    echo 1;
    return
fi

if [[ ! -n "$BUSYBOX" ]]; then
    BUSYBOX=""
fi

attr=`$BUSYBOX lsattr $path | $BUSYBOX cut -f1 -d " "`
attr=`echo $attr | grep "i"`

if [[ -n "$attr" ]]; then
    echo 0
else
    echo 1
fi
