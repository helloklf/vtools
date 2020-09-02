#!/system/bin/sh
source ./kr-script/common/mount.sh

# 从build.prop文件读取prop的值是否为1
function cat_prop_is_1()
{
    prop="$1"
    g="^$prop="
    if [[ -d "$MAGISK_PATH" ]] && [[ -f "$MAGISK_PATH/system.prop" ]]
    then
        status=`grep "$g" $MAGISK_PATH/system.prop | cut -d '=' -f2`
    fi
    if [ "$status" = "1" ] || [ "$status" = "true" ]; then
        echo 1;
        exit 0
    elif [ "$status" = "0" ] || [ "$status" = "false" ]; then
        echo 0;
        exit 0
    fi

    status=`grep "$g" /system/build.prop|cut -d '=' -f2`
    if [ "$status" = "1" ] || [ "$status" = "true" ]; then
        echo 1;
        exit 0
    fi


    if [[ -f "/vendor/build.prop" ]]
    then
        status=`grep "$g" /vendor/build.prop | cut -d '=' -f2`
    fi
    if [ "$status" = "1" ] || [ "$status" = "true" ]; then
        echo 1;
        exit 0
    fi

    echo 0
}

# 从build.prop文件读取prop的值是否为0
function cat_prop_is_0()
{
    prop="$1"
    g="^$prop="
    if [[ -d "$MAGISK_PATH" ]] && [[ -f "$MAGISK_PATH/system.prop" ]]
    then
        status=`grep "$g" $MAGISK_PATH/system.prop | cut -d '=' -f2`
    fi
    if [ "$status" = "0" ] || [ "$status" = "false" ]; then
        echo 1;
        exit 0
    elif [ "$status" = "1" ] || [ "$status" = "true" ]; then
        echo 0;
        exit 0
    fi

    status=`grep "$g" /system/build.prop|cut -d '=' -f2`
    if [ "$status" = "0" ] || [ "$status" = "false" ]; then
        echo 1;
        exit 0
    fi

    if [[ -f "/vendor/build.prop" ]]
    then
        status=`grep "$g" /vendor/build.prop | cut -d '=' -f2`
    fi
    if [ "$status" = "0" ] || [ "$status" = "false" ]; then
        echo 1;
        exit 0
    fi

    echo 0
}

function magisk_set_system_prop() {
    if [[ -d "$MAGISK_PATH" ]];
    then
        echo "你已安装Magisk，本次修改将通过操作进行"
        $BUSYBOX sed -i "/$1=/"d "$MAGISK_PATH/system.prop"
        $BUSYBOX echo "$1=$2" >> "$MAGISK_PATH/system.prop"
        setprop $1 $2 2> /dev/null
        return 1
    fi;
    return 0
}

function set_system_prop() {
    local prop=$1
    local state=$2

    local path="/system/build.prop"
    if [[ -f /vendor/build.prop ]] && [[ -n `cat /vendor/build.prop | grep $prop=` ]]
    then
        local path="/vendor/build.prop"
    fi

    echo '使用本功能，需要解锁system分区，否则修改无效！'
    echo '系统自带的ROOT可能无法使用本功能'

    echo 'Step1.挂载/system为读写'
    mount_all

    $BUSYBOX sed "/$prop=/"d $path > /cache/build.prop
    $BUSYBOX echo "$prop=$state" >> /cache/build.prop
    echo "Step2.修改$prop=$state"

    echo 'Step3.写入文件'
    cp /cache/build.prop $path
    chmod 0755 $path

    echo 'Step4.删除临时文件'
    rm /cache/build.prop
    sync

    echo ''
    echo '重启后生效！'
}