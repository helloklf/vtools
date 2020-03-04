#!/system/bin/sh

if [[ "$ANDROID_UID" = "" ]]
then
    ANDROID_UID="0"
fi

if [[ "$SDCARD_PATH" = "" ]]
then
    SDCARD_PATH="/data/media/$ANDROID_UID"
fi

version828='1346' # 8.2.8 版本开始QQ将数据目录移动到了 Android/data/com.tencent.mobileqq
versionCode=`dumpsys package com.tencent.mobileqq | grep versionCode | cut -f2 -d '=' | cut -f1 -d ' '`

if [[ -d "$SDCARD_PATH/Android/data/com.tencent.mobileqq/Tencent/MobileQQ" ]] && ([[ "$versionCode" -eq "$version828" ]] || [[ "$versionCode" -gt "$version828" ]])
then
    qqsdcard="$SDCARD_PATH/Android/data/com.tencent.mobileqq/Tencent/MobileQQ"
elif [[ -d "$SDCARD_PATH/Tencent" ]]
then
    qqsdcard="$SDCARD_PATH/Tencent/MobileQQ"
else
    qqsdcard="$SDCARD_PATH/tencent/MobileQQ"
fi
qqdata="/data/user/$ANDROID_UID/com.tencent.mobileqq/files"

echo "位于："
echo $qqsdcard
echo "和"
echo $qqdata

echo ''


function lock_dir() {
    local dir="$1"
    ulock_dir "$dir"
    rm -rf "$dir" 2> /dev/null
    echo "" > "$dir"
    chattr +i "$dir" 2> /dev/null
}

function ulock_dir() {
    local dir="$1"
    chattr -i "$dir" 2> /dev/null
    rm -rf "$dir" 2> /dev/null
}

am force-stop com.tencent.mobileqq 2> /dev/null
am kill-all com.tencent.mobileqq 2> /dev/null
am kill com.tencent.mobileqq 2> /dev/null

ulock_dir ${qqsdcard}/font_info
ulock_dir ${qqsdcard}/.font_info
if [[ "$font" = "0" ]];
then
    echo '个性字体：禁用'
    lock_dir ${qqsdcard}/.font_info
    lock_dir ${qqsdcard}/font_info
else
    echo '个性字体：启用'
fi

ulock_dir ${qqdata}/bubble_info
if [[ "$bubble" = "0" ]];
then
    echo '个性气泡：禁用'
    lock_dir ${qqdata}/bubble_info
else
    echo '头像挂架：启用'
fi

ulock_dir ${qqsdcard}/pendant_info
ulock_dir ${qqsdcard}/.pendant
if [[ "$pendant" = "0" ]];
then
    echo '头像挂架：禁用'
    lock_dir ${qqsdcard}/pendant_info
    lock_dir ${qqsdcard}/.pendant
else
    echo '头像挂架：启用'
fi

ulock_dir ${qqsdcard}/.sticker_recommended_pics
if [[ "$sticker" = "0" ]]
then
    echo '表情贴纸：禁用'
    lock_dir ${qqsdcard}/.sticker_recommended_pics
else
    echo '表情贴纸：启用'
fi

ulock_dir ${qqsdcard}/.nomedia
lock_dir ${qqsdcard}/.nomedia

echo ''
echo '操作完成，请重启QQ'
