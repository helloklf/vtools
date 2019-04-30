#!/system/bin/sh

chattr -i $SDCARD_PATH/netease/cloudmusic/Ad 2> /dev/null
$BUSYBOX chattr -i $SDCARD_PATH/netease/cloudmusic/Ad 2> /dev/null
rm -rf $SDCARD_PATH/netease/cloudmusic/Ad 2> /dev/null

if [[ "$state" = "0" ]]; then
    echo "" > $SDCARD_PATH/netease/cloudmusic/Ad
    chattr +i $SDCARD_PATH/netease/cloudmusic/Ad 2> /dev/null
    $BUSYBOX chattr +i $SDCARD_PATH/netease/cloudmusic/Ad 2> /dev/null
fi
