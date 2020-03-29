#!/system/bin/sh

function magisk_set_system_prop() {
    if [[ -d "$MAGISK_PATH" ]];
    then
        echo "你已安装Magisk，本次修改将通过操作进行"
        $BUSYBOX sed -i "/$1=/"d "$MAGISK_PATH/system.prop"
        $BUSYBOX sed -i "\$a$1=$2" "$MAGISK_PATH/system.prop"
        setprop $1 $2 2> /dev/null
        return 1
    fi;
    return 0
}


ANDROID_SDK=`getprop ro.build.version.sdk`

prop="debug.hwui.renderer"
magisk_set_system_prop $prop $renderer

if [[ "$?" = "1" ]];
then
    echo "已通过Magisk更改 $prop，需要重启才能生效！"
else
    echo '使用本功能，需要解锁system分区，否则修改无效！'
    echo 'MIUI自带的ROOT无法使用本功能'

    echo '1.挂载/system为读写'
    source ./kr-script/common/mount.sh
    mount_all

    path="/system/build.prop"
    if [[ -f /vendor/build.prop ]] && [[ -n `cat /vendor/build.prop | grep debug\.hwui\.renderer=` ]]
    then
        path="/vendor/build.prop"
    fi

    $BUSYBOX sed "/$prop=/"d $path > /cache/build.prop

    $BUSYBOX sed -i "\$a$prop=$renderer" /cache/build.prop
    echo "2.修改debug.hwui.renderer=$renderer"

    echo '3.覆盖/system/build.prop'
    cp /cache/build.prop $path

    echo '4.修正读写权限'
    chmod 0755 $path

    echo '5.删除临时文件'
    rm /cache/build.prop
    sync
    echo ''
    echo '重启后生效！'

    setprop $prop $renderer
fi

if [[ "$ANDROID_SDK" = 28 ]]
then
    if [[ "$renderer" = "opengl" ]]
    then
        echo '在Android P系统使用OpenGL作为HWUI渲染引擎，可能会导致Webview异常，打开网页时闪退！' 1>&2
    elif [[ "$renderer" = "skiavk" ]]
    then
        echo '在Android P系统使用Skia Vulkan作为HWUI渲染引擎，可能导致Webview界面红屏，视频播放界面蓝屏，无法截图' 1>&2
    fi
fi
