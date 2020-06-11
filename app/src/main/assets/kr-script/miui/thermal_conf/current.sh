dir="${MAGISK_PATH}/system/vendor/etc"

if [[ -f "$dir/thermal-engine.current.ini" ]]; then
    # 旧版
    mode=`cat "$dir/thermal-engine.current.ini"`
elif [[ -f "$dir/thermal.current.ini" ]]; then
    # 新版
    mode=`cat "$dir/thermal.current.ini"`
else
    mode=''
fi

echo $mode