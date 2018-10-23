#!/system/bin/sh

if [[ "$ANDROID_UID" = "" ]]
then
    ANDROID_UID="0"
fi

if [[ "$SDCARD_PATH" = "" ]]
then
    SDCARD_PATH="/data/media/$ANDROID_UID"
fi

echo "位于："
qqsdcard="$SDCARD_PATH/tencent/MobileQQ"
qqdata="/data/user/$ANDROID_UID/com.tencent.mobileqq/files"
echo ""

echo $qqsdcard
echo $qqdata

if [[ $1 = '0' ]]
then
    rm -rf ${qqsdcard}/.font_info
    echo "" > ${qqsdcard}/.font_info
    rm -rf ${qqsdcard}/font_info
    echo "" > ${qqsdcard}/font_info
    rm -rf ${qqsdcard}/.font_info
    echo "" > ${qqsdcard}/.font_info
    rm -rf ${qqsdcard}/font_info
    echo "" > ${qqsdcard}/font_info
    rm -rf ${qqdata}/bubble_info
    echo "" > ${qqdata}/bubble_info
    rm -rf ${qqdata}/pendant_info
    echo "" > ${qqdata}/pendant_info
    rm -rf ${qqsdcard}/.pendant
    echo "" > ${qqsdcard}/.pendant
    rm -rf ${qqsdcard}/.pendant
    echo "" > ${qqsdcard}/.pendant
    rm -rf ${qqsdcard}/.nomedia
    echo "" > ${qqsdcard}/.nomedia
    echo '已移除QQ个性化样式'
else
    rm -rf ${qqsdcard}/font_info
    mkdir -p ${qqsdcard}/font_info
    rm -rf ${qqsdcard}/.font_info
    rm -rf ${qqsdcard}/.pendant
    rm -rf ${qqdata}/bubble_info
    mkdir -p ${qqdata}/bubble_info
    rm -rf ${qqdata}/pendant_info
    echo '已恢复QQ个性化样式'
fi
am force-stop com.tencent.mobileqq 2> /dev/null
am kill-all com.tencent.mobileqq 2> /dev/null
am kill com.tencent.mobileqq 2> /dev/null
echo '操作完成，请重启QQ'
