#!/system/bin/sh

if [[ -n "$platform" ]]
then
    echo "处理器平台：$platform"
else
    echo '暂不支持该处理器' 1>&2
    exit 3
fi

if [[ ! -n "$MAGISK_PATH" ]]
then
    echo 'Scene 附加模块未启用，请先前往Magisk助手初始化模块' 1>&2
    exit 4
fi

dir="${MAGISK_PATH}/system/vendor/etc"

current_save="$dir/thermal-engine.current.ini"
config="./custom/actions/miui_thermal_conf/${platform}-${brightnessMode}.conf"

function replace_file() {
    if [[ "$platform" = "msmnile" ]]
    then
        cp "$1" "$dir/thermal$2.conf"
        chmod 755 "$dir/thermal$2.conf"
    else
        cp "$1" "$dir/thermal-engine-${platform}$2.conf"
        chmod 755 "$dir/thermal-engine-${platform}$2.conf"
    fi
}
function remove_file() {
    if [[ "$platform" = "msmnile" ]]
    then
        if [[ -f "$dir/thermal$1.conf" ]]
        then
            rm -f "$dir/thermal$1.conf"
        fi
    else
        rm -f "$dir/thermal-engine-${platform}$1.conf"
    fi
}

function lock_dir() {
    local dir="$1"
    if [[ -d "$dir" ]]
    then
        rm -rf "$dir" 2> /dev/null
        echo '' > "$dir"
        chattr +i "$dir" 2> /dev/null
    fi
}

function ulock_dir() {
    local dir="$1"
    if [[ -f "$dir" ]]
    then
        chattr -i "$dir" 2> /dev/null
        rm -rf "$dir" 2> /dev/null
    fi
}

function replace_configs()
{
    if [[ -f "$config" ]]
    then
        echo '## 开始执行'
    echo ""
    else
        echo '所需的资源文件已丢失！！！' 1>&2
        exit 2
    fi

    mkdir -p "$MAGISK_PATH/system/vendor/etc"

    echo '注意：如果你曾使用其它方式删除过修改了温控，此功能可能不会有效！！！'
    echo ''

    replace_file "$config" ""
    replace_file "$config" "-normal"
    replace_file "$config" "-camera"

    if [[ "$platform" = "msmnile" ]]
    then
        if [[ "$brightnessMode" = "danger" ]]
        then
            echo "# empty\n" > "$dir/thermal-chg-only.conf"
        else
            remove_file "-chg-only"
        fi
        replace_file "$config" "-devices"
        replace_file "$config" "-engine"
    fi

    replace_file "$config" "-class0"
    replace_file "$config" "-map"
    replace_file "$config" "-phone"
    replace_file "$config" "-pubgmhd"
    replace_file "$config" "-sgame"
    replace_file "$config" "-tgame"
    replace_file "$config" "-high"
    replace_file "$config" "-extreme"
    replace_file "$config" "-nolimits"

    lock_dir /data/thermal
    lock_dir /data/vendor/thermal

    echo '## 重启手机后生效'
}

function remove_configs()
{
    echo '本操作只能还原通过“本功能”对温控的修改！'

    remove_file ""
    remove_file "-normal"
    remove_file "-camera"

    if [[ "$platform" = "msmnile" ]]
    then
        remove_file "-chg-only"
        remove_file "-devices"
        remove_file "-engine"
    fi

    remove_file "-class0"
    remove_file "-high"
    remove_file "-map"
    remove_file "-phone"
    remove_file "-pubgmhd"
    remove_file "-sgame"
    remove_file "-tgame"
    remove_file "-extreme"
    remove_file "-nolimits"

    ulock_dir /data/thermal
    ulock_dir /data/vendor/thermal

    rm -rf "$current_save"

    echo '## 重启手机后生效'
}

case "$brightnessMode" in
    "default")
        remove_configs
        exit 0
     ;;
    "high")
        echo '此模式下，降频温度阈值被提高，性能更稳定'
        echo ''
    ;;
    "nolimits")
        echo '在此模式下，性能被最大化释放，保留了充电温度控制'
        echo ''
    ;;
    "danger")
        echo '此模式下，充电速度和SOC性能不会被温度限制' 1>&2
        echo '留意发热，谨防爆炸！！！' 1>&2
    ;;
    *)
        echo '错误，选择的模式无效' 1>&2
        exit 1
    ;;
esac

replace_configs "$brightnessMode"
echo "$brightnessMode" > "$current_save"
