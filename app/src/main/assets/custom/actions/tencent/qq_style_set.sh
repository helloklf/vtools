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

echo "位于："
echo $qqsdcard
echo "和"
echo $qqdata

echo ''

am force-stop com.tencent.mobileqq 2> /dev/null
am kill-all com.tencent.mobileqq 2> /dev/null
am kill com.tencent.mobileqq 2> /dev/null

rm -rf ${qqsdcard}/font_info
rm -rf ${qqsdcard}/.font_info

if [[ "$font" = "0" ]];
then
    echo '个性字体：禁用'
    echo "" > ${qqsdcard}/.font_info
    echo "" > ${qqsdcard}/font_info
else
    echo '个性字体：启用'
fi

rm -rf ${qqdata}/bubble_info
if [[ "$bubble" = "0" ]];
then
    echo '个性气泡：禁用'
    echo "" > ${qqdata}/bubble_info
else
    echo '头像挂架：启用'
fi

rm -rf ${qqdata}/pendant_info
if [[ "$pendant" = "0" ]];
then
    echo '头像挂架：禁用'
    echo "" > ${qqsdcard}/.pendant
else
    echo '头像挂架：启用'
fi

rm -rf ${qqsdcard}/.nomedia
echo "" > ${qqsdcard}/.nomedia

echo ''
echo '操作完成，请重启QQ'
