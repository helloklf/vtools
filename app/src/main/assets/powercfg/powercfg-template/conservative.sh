#!/system/bin/sh

action=$1
if [[ "$action" = "init" ]] && [[ -f '/data/powercfg-base.sh' ]]; then
    sh /data/powercfg-base.sh
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
