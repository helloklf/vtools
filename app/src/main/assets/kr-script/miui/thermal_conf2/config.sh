platform=`getprop ro.board.platform`
mode="$state"

if [[ "$MAGISK_PATH" == "" ]]; then
  MAGISK_PATH="/data/adb/modules/scene_systemless"
fi

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

    echo '建议稍后重启手机' 1>&2
    echo '#################################'
  fi
}

function uninstall_thermal() {
  clear_old

  echo "从 $install_dir 目录"
  echo '卸载已安装的自定义配置……'
  echo ''

  if [[ -d "$install_dir" ]]; then
    chattr -R -i "$install_dir" # 2> /dev/null
    rm -rf "$install_dir/*" 2> /dev/null
  elif [[ -f "$install_dir" ]]; then
    chattr -i "$install_dir" # 2> /dev/null
    rm -f "$install_dir"
    echo '系统目录遭到破坏，切换温控可能无法顺利进行' 1>&2
    echo '你大概需要重启手机，温控配置才[有可能]生效~' 1>&2
    sleep 8
  fi

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
  echo '#################################'
  cat $resource_dir/info.txt
  echo ''
  echo '#################################'
  echo ''
  echo ''

  if [[ ! -d "$install_dir" ]]; then
    mkdir -p "$install_dir"
  fi

  for thermal in ${thermal_files[@]}; do
    if [[ -f "$resource_dir/$thermal" ]]; then
      echo '复制' $thermal
      cp "$resource_dir/$thermal" "$install_dir/$thermal"
      chmod 644 "$install_dir/$thermal"
    elif [[ -f "$resource_dir/general.conf" ]]; then
      echo '复制' $thermal
      cp "$resource_dir/general.conf" "$install_dir/$thermal"
      chmod 644 "$install_dir/$thermal"
    fi
    dos2unix "$install_dir/$thermal" 2> /dev/null
  done
  echo "$mode" > "$mode_state_save"

  echo 'OK~'
  echo ''
  echo '留意，如果你使用的不是官方原版系统，或曾使用其它工具或模块更改温控，此操作可能不会生效'
  echo '如遇温控切换不生效情况，可尝试重启手机，如果重启后依然无效，那……' 1>&2
}

if [[ "$mode" == "default" ]]; then
  uninstall_thermal
elif [[ -d $resource_dir ]]; then
  install_thermal
else
  echo '错误，选择的模式'$mode'无效' 1>&2
  exit 1
fi
