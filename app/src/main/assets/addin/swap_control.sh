#!/system/bin/sh

swapfile=/data/swapfile
action="$1"     # 操作(脚本中的函数名)
loop="$2"       # 是否挂载为loop设备
priority="$3"   # 优先级
swapsize="$4"   # SWAP大小MB

loop_save="vtools.swap.loop"

# 获取下一个loop设备的索引
function get_next_loop() {
    local current_loop=`getprop $loop_save`

    if [[ "$current_loop" != "" ]]
    then
        return "$current_loop"
    fi

    local loop_index=0
    # local used=`blkid | grep /dev/block/loop | cut -f1 -d ":"`
    local used=`blkid | grep /dev/block/loop`
    for loop in /dev/block/loop*
    do
        if [[ "$loop_index" -gt "0" ]]
        then
            if [[ `echo $used | grep /dev/block/loop$loop_index` = "" ]]
            then
                return $loop_index
            fi
        else
            local loop_index=`expr $loop_index + 1`
        fi
    done

    return 5
}

if [[ $loop == "1" ]]; then
    get_next_loop
    next_loop_index="$?"
    swap_mount="/dev/block/loop$next_loop_index"
else
    swap_mount=$swapfile
fi


# 关闭swap（如果正在使用，那可不是一般的慢）
function diable_swap() {
	swapoff $swap_mount >/dev/null 2>&1

    if [[ $loop == "1" ]]; then
        losetup -d $swap_mount >/dev/null 2>&1
    fi

	setprop $loop_save ""
}

# 开启SWAP
function enable_swap() {
    if [[ ! -f $swapfile ]]
    then
        if [[ "$swapsize" = "" ]]
        then
            swapsize=256
        fi
        dd if=/dev/zero of=$swapfile bs=1048576 count=$swapsize # 创建
    fi

    if [[ $loop == "1" ]]; then
        # losetup $swap_mount $swapfile # 挂载
        if [[ -e $swap_mount ]]
        then
            losetup -d $swap_mount      # 删除loop设备
        fi
        losetup $swap_mount $swapfile   # 挂载为loop设备
        setprop $loop_save $next_loop_index
    fi

	mkswap $swap_mount >/dev/null 2>&1 # 初始化
	if [[ "$priority" != "" ]]
	then
	    swapon $swap_mount -p "$priority" >/dev/null 2>&1   # 开启

        if [[ "$priority" == '0' ]]; then
            # zram_priority=`cat /proc/swaps | grep /zram0 | sed 's/[ \t][ ]*/,/g' | cut -f5 -d ','`
            zram_info=`cat /proc/swaps | grep /zram0`
            if [[ $zram_info != "" ]]; then
                zram_priority=`echo $zram_info | sed 's/[ \t][ ]*/,/g' | cut -f5 -d ','`
                if [[ $zram_priority != $priority ]]; then
                    swapoff /dev/block/zram0
                    swapon /dev/block/zram0 -p $priority
                fi
            fi
        fi
    else
	    swapon $swap_mount >/dev/null 2>&1                  # 开启
	fi

	echo 100 > /proc/sys/vm/overcommit_ratio
	echo 100 > /proc/sys/vm/swap_ratio
	echo 0 > /proc/sys/panic_on_oom
}

"$action"
