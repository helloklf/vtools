#!/system/bin/sh

install_dir="${MAGISK_PATH}/system/vendor/etc"
# 方案2 - 替换到 /data
install_dir="/data/vendor/thermal/config"

if [[ -f "$install_dir/thermal-engine.current.ini" ]]; then
    # 旧版
    mode=`cat "$install_dir/thermal-engine.current.ini" | cut -f1 -d '_'`
elif [[ -f "$install_dir/thermal.current.ini" ]]; then
    # 新版
    mode=`cat "$install_dir/thermal.current.ini" | cut -f1 -d '_'`
else
    mode=''
fi

modename=""
case "$mode" in
    "default")
        modename="系统默认 (default)"
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

echo -n "当前："
platform=`getprop ro.board.platform`

if [[ "$platform" = "sdm845" ]]
then
    echo -n '骁龙845'


elif [[ "$platform" = "sdm710" ]]
then
    echo -n '骁龙710AIE'


elif [[ "$platform" = "kona" ]]
then
    echo -n '骁龙865'


elif [[ "$platform" = "msmnile" ]]
then
    echo -n '骁龙855'


elif [[ "$platform" = "sm6150" ]]
then
    echo -n '骁龙730'


elif [[ "$platform" = "msm8998" ]]
then
    echo -n '骁龙835'


elif [[ "$platform" = "msm8996" ]]
then
    echo -n '骁龙820\821'


elif [[ "$platform" = "mt6873" ]]
then
    echo -n '天玑800\820'
fi


echo " $modename"