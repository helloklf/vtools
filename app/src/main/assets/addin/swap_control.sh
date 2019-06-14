#!/system/bin/sh

swapfile=/data/swapfile
action="$1"     # 操作(脚本中的函数名)
priority="$2"   # 优先级
swapsize="$3"   # SWAP大小MB
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

get_next_loop
next_loop_index="$?"
swap_mount="/dev/block/loop$next_loop_index"

# swap_mount="/dev/block/loop5"

# 关闭swap（如果正在使用，那可不是一般的慢）
function diable_swap() {
	swapoff $swap_mount >/dev/null 2>&1
	losetup -d $swap_mount >/dev/null 2>&1
	setprop $loop_save ""
}

# 挂载SWAP为loop设备
function swapfile_losetup() {
    if [[ -e $swap_mount ]]
    then
	    losetup -d $swap_mount      # 删除loop设备
    fi
	losetup $swap_mount $swapfile   # 挂载为loop设备
	setprop $loop_save $next_loop_index
}

# 开启SWAP
function enable_swap() {
    # diable_swap
    if [[ ! -f $swapfile ]]
    then
        if [[ "$swapsize" = "" ]]
        then
            swapsize=256
        fi
        dd if=/dev/zero of=$swapfile bs=1048576 count=$swapsize # 创建
    fi

	# losetup $swap_mount $swapfile # 挂载
	swapfile_losetup

	mkswap $swap_mount >/dev/null 2>&1 # 初始化
	if [[ "$priority" != "" ]]
	then
	    swapon $swap_mount -p "$priority" >/dev/null 2>&1   # 开启
    else
	    swapon $swap_mount >/dev/null 2>&1                  # 开启
	fi
}

"$action"
