#!/system/bin/sh

action=$1

init () {
  local dir=$(cd $(dirname $0); pwd)
  if [[ -f "$dir/powercfg-base.sh" ]]; then
    sh "$dir/powercfg-base.sh"
  elif [[ -f '/data/powercfg-base.sh' ]]; then
    sh /data/powercfg-base.sh
  fi
}
if [[ "$action" = "init" ]]; then
  init
	exit 0
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
