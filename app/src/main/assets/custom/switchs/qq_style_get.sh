#!/system/bin/sh

if [[ "$ANDROID_UID" = "" ]]
then
    ANDROID_UID="0"
fi

if [[ "$SDCARD_PATH" = "" ]]
then
    SDCARD_PATH="/data/media/$ANDROID_UID"
fi

qqsdcard="$SDCARD_PATH/tencent/MobileQQ"
qqdata="/data/user/$ANDROID_UID/com.tencent.mobileqq/files"

if [[ -d ${qqsdcard}/.font_info ]]; then
    echo 1
elif [[ -d ${qqsdcard}/font_info ]]; then
    echo 1
elif [[ -d ${qqsdcard}/bubble_info ]]; then
    echo 1
else
    echo 0
fi
