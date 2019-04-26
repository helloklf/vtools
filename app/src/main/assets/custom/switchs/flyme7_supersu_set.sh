#!/system/bin/sh

function mount_systemdir() {
    mount -o rw,remount /system
    mount -o rw,remount /
    mkdir -p /system/app/SuperSu
    mkdir -p /system/bin/.ext
}

function download_supersu() {
    echo "下载地址：\nhttp://atools.oss-cn-shenzhen.aliyuncs.com/app1/SuperSU_ForFlyme7.apk"
    sleep 2
    am start -a android.intent.action.VIEW -d "http://atools.oss-cn-shenzhen.aliyuncs.com/app1/SuperSU_ForFlyme7.apk"
}

function copy_files() {
    if [[ ! -d ./custom/switchs/flyme7 ]]
    then
        echo '所需要的资源文件丢失，无法继续操作...'
        exit
    fi
    cd ./custom/switchs/flyme7

    if [[ -f 'libsupol.so' ]] && [[ -f 'supolicy' ]] && [[ -f 'su' ]]
    then
        echo '资源文件已准备就绪'
        echo '开始复制文件并切换SuperSU...'
    else
        echo '所需要的资源文件丢失，无法继续操作...'
        exit
    fi

    cp libsupol.so /system/lib/
    cp libsupol.so /system/lib64/
    cp su /system/bin/.ext/.su
    cp supolicy /system/xbin/
    cp su /system/xbin/daemonsu
    cp su /system/
    cp su /system/etc/su
    # cp SU.apk /system/app/SuperSu/SuperSU.apk
    echo -e "\n/system/xbin/daemonsu --auto-daemon & ">>/system/bin/install-recovery.sh
    cp /system/bin/install-recovery.sh /system/etc/

    chmod 0644 /system/lib/libsupol.so
    chmod 0644 /system/lib64/libsupol.so
    chmod 0755 /system/bin/.ext
    chmod 0755 /system/bin/.ext/.su
    chmod 0755 /system/xbin/supolicy
    chmod 6755 /system/xbin/daemonsu
    chmod 6755 /system/su
    chmod 6755 /system/etc/su
    chmod 0755 /system/app/SuperSu
    chmod 0644 /system/app/SuperSu/SuperSU.apk

    mv /system/su /system/xbin/su
    mv /system/etc/su /system/bin/su
    /system/xbin/daemonsu --auto-daemon & am start -n eu.chainfire.supersu/.MainActivity

    echo '脚本执行完成，如果没有报错，理论上就可以了~~'
    # echo "脚本执行完成，系统将在2秒后重启"
    # sleep 2
    # reboot
}

if [[ $1 = "1" ]]
then
    if [ -f "/system/xbin/daemonsu" ]
    then
        echo 'SuperSU已启用，不需要重复操作！'
        exit
    fi

    if [ ! -d "/data/data/eu.chainfire.supersu" ]; then
        echo "请先下载并安装SuperSU"
        echo "安装完成后再进行本操作！"
        download_supersu
        exit
    fi

    mount_systemdir
    if [ ! -d "/system/bin/.ext" ]; then
        echo ""
        echo "无法修改System分区..."
        exit
    else
        copy_files
        echo '现在，你可能需要重启手机~'
    fi
else
    echo '暂不支持取消使用SuperSU！'
fi