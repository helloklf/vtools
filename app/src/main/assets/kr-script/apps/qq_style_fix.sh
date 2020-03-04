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

cd /cache

function ulock_dir() {
    local dir="$1"
    chattr -R -i "$dir" 2> /dev/null
    rm -rf "$dir" 2> /dev/null
}

am force-stop com.tencent.mobileqq 2> /dev/null
am kill-all com.tencent.mobileqq 2> /dev/null
am kill com.tencent.mobileqq 2> /dev/null

echo '删除数据...'
ulock_dir $qqsdcard
ulock_dir $qqdata

echo '卸载QQ...'
pm uninstall com.tencent.mobileqq

echo '操作完成，请重新安装QQ~'