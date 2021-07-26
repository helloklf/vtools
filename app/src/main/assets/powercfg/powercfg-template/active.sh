action=$1

init () {
  local dir=$(cd $(dirname $0); pwd)
  if [[ -f "$dir/powercfg-base.sh" ]]; then
    sh "$dir/powercfg-base.sh"
  elif [[ -f '/data/powercfg-base.sh' ]]; then
    sh /data/powercfg-base.sh
  fi
}

if [[ "$action" == "init" ]]; then
  init
  exit 0
fi


# 当前被打开的前台应用（需要Scene 4.3+版本，并开启【严格模式】才会获得此值）
if [[ "$top_app" != "" ]]; then
  echo "应用切换到前台 [$top_app]" >> /cache/scene_powercfg.log
fi

if [[ "$action" = "powersave" ]]; then
    #powersave

  exit 0
elif [[ "$action" = "balance" ]]; then
  #balance

  exit 0
elif [[ "$action" = "performance" ]]; then
  #performance

  exit 0
elif [[ "$action" = "fast" ]]; then
  #fast

  exit 0
fi
