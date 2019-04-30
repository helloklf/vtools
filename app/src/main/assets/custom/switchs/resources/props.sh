#!/system/bin/sh

# 从build.prop文件读取prop的值是否为1
function cat_prop_is_1()
{
    prop="$1"
    status=`grep $prop /system/build.prop|cut -d '=' -f2`
    if [ "$status" = "1" ]; then
        echo 1;
        exit 0
    fi

    status=`grep $prop /vendor/build.prop|cut -d '=' -f2`
    if [ "$status" = 1 ]; then
        echo 1;
    else
        echo 0;
    fi;
}