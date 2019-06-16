#!/system/bin/sh

source ./custom/common/mount.sh
source ./custom/common/magisk.sh

if [[ $action = "view" ]]; then
    echo "位于/vendor目录：" 1>&2
    for file in `find /vendor -name thermal* -type f`; do
        echo $file
    done

    echo "位于/system目录：" 1>&2
    for file in `find /system -name thermal* -type f`; do
        echo $file
    done
elif [[ $action = "delete" ]]; then
    module_installed # 检查是否使用了magisk模块
    mg="$?"
    if [[ $mg = "1" ]] && [[ -f "${MAGISK_PATH}/system/vendor/etc/thermal-engine.current.ini" ]]; then
        echo "你已使用过MIUI专属的“更改温控配置”" 1>&2
        echo "大多数情况下，温控切换比直接删除要可靠" 1>&2
        echo "如果你一定要删除温控，请先将“更改温控配置”调为默认，并重启手机" 1>&2
        exit 1
    fi

    echo "注意：本操作可能会导致系统崩溃或出现未知问题，" 1>&2
    echo "并且本软件也不提供恢复操作，请务必留有完整刷机包，" 1>&2
    echo "或备份system和vendor分区，必要时通过刷机或还原备份来恢复温控！" 1>&2
    echo '如果你还没准备好，请点击右侧“退出”按钮'
    echo '如果继续，操作将在30秒后开始...'

    sleep 1
    for i in 1 2 3 4 5 6 7 8
    do
        echo "progress:[$i/9]"
        sleep 3
    done

    mount_all # 挂载目录为读写

    for file in `find /vendor -name thermal* -type f`; do
        rm -f $file
    done

    for file in `find /vendor -name thermal* -type f`; do
        rm -f $file
    done

    for file in `find /system -name thermal* -type f`; do
        rm -f $file
    done

    echo '如果没有出现错误提示，现在重启手机就可以了...'
fi