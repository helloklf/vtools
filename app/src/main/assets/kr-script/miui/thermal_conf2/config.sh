platform=`getprop ro.board.platform`
mode="$state"

if [[ ! -n "$MAGISK_PATH" ]]; then
    echo 'Scene 附加模块未启用，请先前往Magisk助手初始化模块' 1>&2
    exit 4
fi

install_dir="${MAGISK_PATH}/system/vendor/etc"
# 方案2 - 替换到 /data
install_dir="/data/vendor/thermal/config"
mode_state_save="$install_dir/thermal.current.ini"
resource_dir="./kr-script/miui/thermal_conf2/$platform/$mode"

thermal_files=(
)

# 覆盖 thermal_files
source ./kr-script/miui/thermal_conf2/$platform/thermal_files.sh

function clear_old() {
  old_dir="${MAGISK_PATH}/system/vendor/etc"
  old_state_save="$old_dir/thermal.current.ini"
  if [[ -f $old_state_save ]]; then
    echo '清理旧版文件'
    for thermal in ${thermal_files[@]}; do
      if [[ -f $old_dir/$thermal ]]; then
        rm -f $old_dir/$thermal
      fi
    done
    rm -f "$old_state_save" 2> /dev/null

    echo '建议稍候重启手机' 1>&2
    echo '#################################'
  fi
}

function uninstall_thermal() {
    clear_old

    echo "从 $install_dir 目录"
    echo '卸载已安装的自定义配置……'
    echo ''

    # ulock_dir /data/thermal
    # ulock_dir /data/vendor/thermal
    # for thermal in ${thermal_files[@]}; do
    #     if [[ -f $install_dir/$thermal ]]; then
    #         echo '移除' $thermal
    #         rm -f $install_dir/$thermal
    #     fi
    # done

    rm $install_dir/* 2>/dev/null
    rm -f "$mode_state_save" 2> /dev/null

    echo ''
}

function install_thermal() {
    uninstall_thermal

    echo '检测模块间是否存在冲突……'
    echo ''

    # 检查其它模块是否更改温控
    local magisk_dir=`echo $MAGISK_PATH | awk -F '/[^/]*$' '{print $1}'`
    local modules=`ls $magisk_dir`
    for module in ${modules[@]}; do
        if [[ ! "$magisk_dir/$module" = "$MAGISK_PATH" ]] && [[ -d "$magisk_dir/$module" ]] && [[ ! -f "$magisk_dir/$module/disable" ]]; then
            local result=`find "$magisk_dir/$module" -name "*thermal*" -type f`
            if [[ -n "$result" ]]; then
                echo '发现其它修改温控的模块：' 1>&2
                echo "$result" 1>&2
                echo '请删除以上位置的文件，或禁用相关模块！' 1>&2
                echo '否则，Scene无法正常替换系统温控！' 1>&2
                exit 5
            fi
        fi
    done

    echo ''
    echo ''
    echo '#################################'
    cat $resource_dir/info.txt
    echo '#################################'
    echo ''
    echo ''
    echo ''

    if [[ ! -d "$install_dir" ]]; then
        mkdir -p "$install_dir"
    fi

    for thermal in ${thermal_files[@]}; do
        if [[ -f "$resource_dir/$thermal" ]]; then
            echo '复制' $thermal
            cp "$resource_dir/$thermal" "$install_dir/$thermal"
        elif [[ -f "$resource_dir/general.conf" ]]; then
            echo '复制' $thermal
            cp "$resource_dir/general.conf" "$install_dir/$thermal"
        fi
    done
    echo "$mode" > "$mode_state_save"

    echo 'OK~'
}

case "$mode" in
  "default")
      uninstall_thermal
   ;;
  *)
    if [[ -d $resource_dir ]]; then
      install_thermal
    else
      echo '错误，选择的模式'$mode'无效' 1>&2
      exit 1
    fi
  ;;
esac
