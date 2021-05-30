dir="${MAGISK_PATH}/system/vendor/etc"
if [[ -f "$dir/thermal-engine.current.ini" ]]; then
    # 旧版
    mode=`cat "$dir/thermal-engine.current.ini" | cut -f1 -d '_'`
elif [[ -f "$dir/thermal.current.ini" ]]; then
    # 新版
    mode=`cat "$dir/thermal.current.ini" | cut -f1 -d '_'`
else
    mode=''
fi

modename=""
case "$mode" in
  "default")
    modename="系统默认 (default)"
   ;;
  "cool")
    modename="清爽酷凉 (cool)"
   ;;
  "powerfrugal")
    modename="节能降温 (powerfrugal)"
   ;;
  "performance")
    modename="提高阈值 (performance)"
  ;;
  "slight")
    modename="轻微调整 (slight)"
  ;;
  "pro")
    modename="深度定制 (pro)"
  ;;
  "author")
    modename="游戏嘟日常 (author)"
  ;;
  "extreme")
    modename="极致性能 (extreme)"
  ;;
  "danger")
    modename="丧心病狂 (danger)"
  ;;
  "game")
    modename="游戏模式 (game)"
  ;;
  "")
    modename="未替换"
  ;;
  *)
    # modename="未替换"
  ;;
esac

echo "当前：$modename"