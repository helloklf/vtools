#!/system/bin/sh

source ./custom/common/magisk.sh

# 混合模式替换文件(如果有magisk就用magisk，否则就用root直接替换系统文件)
# mixture_hook_file "./custom/switchs/resources/com.android.systemui" "/system/media/theme/default/com.android.systemui" "$mode"
# $mode 可是 1 或者 0，1表示替换，0表示取消替换
function mixture_hook_file()
{
    local resource="$1"
    local output="$2"
    local mode="$3"

    if [[ -f $resource ]]
    then
        module_installed
        mg="$?"

        if [[ "$mg" = 1 ]]
        then
            echo '本操作将通过Magisk进行'
            if [[ $mode = 1 ]]
            then
                magisk_replace_file $resource $output
                success="$?"
                if [[ "$success" = 1 ]]
                then
                    echo '操作成功，请重启手机！'
                else
                    echo '操作失败...' 1>&2
                fi
            else
                magisk_cancel_replace $output
                success="$?"
                if [[ "$success" = 1 ]]
                then
                    if [[ -f $output ]] && [[ -f $resource ]]
                    then
                        local md5=`busybox md5sum $resource | cut -f1 -d ' '`
                        local verify=`busybox md5sum $output | cut -f1 -d ' '`

                        if [[ "$md5" = "$verify" ]]
                        then
                            echo '需要重启手机才会生效！' 1>&2
                        else
                            echo '需要重启手机才会生效！'
                        fi
                    fi
                else
                    echo '操作失败...' 1>&2
                fi
            fi
        else
            echo '使用本功能，需要解锁system分区，否则修改无效！'
            echo 'MIUI自带的ROOT无法使用本功能'

            echo '挂载/system为读写'

            if [[ $mode = 1 ]]
            then
                $BUSYBOX mount -o rw,remount /system
                mount -o rw,remount /system
                $BUSYBOX mount -o remount,rw /dev/block/bootdevice/by-name/system /system
                mount -o remount,rw /dev/block/bootdevice/by-name/system /system 2> /dev/null

                $BUSYBOX mount -o rw,remount /vendor 2> /dev/null
                mount -o rw,remount /vendor 2> /dev/null
                $BUSYBOX mount -o rw,remount /system/vendor 2> /dev/null
                mount -o rw,remount /system/vendor 2> /dev/null

                if [[ -e /dev/block/bootdevice/by-name/vendor ]]; then
                    $BUSYBOX mount -o rw,remount /dev/block/bootdevice/by-name/vendor /vendor 2> /dev/null
                    mount -o rw,remount /dev/block/bootdevice/by-name/vendor /vendor 2> /dev/null
                fi

                if [[ -f "$output" ]] && [[ ! -f "$output.bak" ]]
                then
                    cp "$output" "$output.bak"
                fi
                cp $resource $output
                chmod 0755 $output
            else
                if [[ -f "$output.bak" ]]
                then
                    cp "$output.bak" "$output"
                fi
                rm -f $output
            fi
            echo '需要重启才能生效~'
        fi

    else
        echo '所需的资源文件缺失，无法进行操作' 1>&2
    fi

}

# 是否已用混合模式替换了文件(如果有magisk就用magisk，否则就用root直接替换系统文件)
# file_mixture_hooked "./custom/switchs/resources/com.android.systemui" "/system/media/theme/default/com.android.systemui"
# @return 【1】或【0】
function file_mixture_hooked()
{
    local resource="$1"
    local output="$2"
    if [[ ! -f $resource ]]
    then
        return 0
        exit 0
    fi

    magisk_file_exist $output
    exist="$?"
    magisk_file_equals $resource $output
    equals="$?"

    if [[ $exist = 1 ]] && [[ $equals = 1 ]]
    then
        return 1
    else
        if [[ -f $output ]] && [[ -f $resource ]]
        then
            local md5=`busybox md5sum $resource | cut -f1 -d ' '`
            local verify=`busybox md5sum $output | cut -f1 -d ' '`

            if [[ "$md5" = "$verify" ]]
            then
                return 1
            fi
        fi
    fi

    return 0
}