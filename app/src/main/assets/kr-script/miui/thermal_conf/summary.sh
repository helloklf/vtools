#!/system/bin/sh

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

modename=""
case "$mode" in
    "default")
        modename="系统默认 (default)"
     ;;
    "high")
        modename="提高阈值 (旧版配置)"
    ;;
    "high2")
        modename="稳定性能 (旧版配置)"
    ;;
    "nolimits")
        modename="极致性能 (旧版配置)"
    ;;
    "performance")
        modename="提高阈值 (performance)"
    ;;
    "extreme")
        modename="极致性能 (extreme)"
    ;;
    "danger")
        modename="丧心病狂 (danger)"
    ;;
    *)
        modename="未替换"
    ;;
esac

echo "当前：$modename"