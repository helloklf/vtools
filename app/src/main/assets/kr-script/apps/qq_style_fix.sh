#!/system/bin/sh

if [[ "$ANDROID_UID" = "" ]]
then
    ANDROID_UID="0"
fi

if [[ "$SDCARD_PATH" = "" ]]
then
    SDCARD_PATH="/data/media/$ANDROID_UID"
fi

if [[ -d "$SDCARD_PATH/Tencent" ]]
then
    qqsdcard="$SDCARD_PATH/Tencent/MobileQQ"
else
    qqsdcard="$SDCARD_PATH/tencent/MobileQQ"
fi
qqdata="/data/user/$ANDROID_UID/com.tencent.mobileqq"

echo "位于："
echo $qqsdcard
echo "和"
echo $qqdata

echo ''

cd /cache

function ulock_dir() {
    local dir="$1"
    chattr -R -i "$dir" 2> /dev/null
    rm -rf "$dir" 2> /dev/null
}

am force-stop com.tencent.mobileqq 2> /dev/null
am kill-all com.tencent.mobileqq 2> /dev/null
am kill com.tencent.mobileqq 2> /dev/null

ulock_dir $qqsdcard
ulock_dir $qqdata

echo '操作完成，请重新启动QQ~'