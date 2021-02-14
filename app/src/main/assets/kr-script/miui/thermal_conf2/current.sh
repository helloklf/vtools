install_dir="${MAGISK_PATH}/system/vendor/etc"
# 方案2 - 替换到 /data
install_dir="/data/vendor/thermal/config"

if [[ -f "$install_dir/thermal-engine.current.ini" ]]; then
    # 旧版
    mode=`cat "$install_dir/thermal-engine.current.ini"`
elif [[ -f "$install_dir/thermal.current.ini" ]]; then
    # 新版
    mode=`cat "$install_dir/thermal.current.ini"`
else
    mode=''
fi

echo $mode