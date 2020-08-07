#! /vendor/bin/sh

if [[ -f /system/bin/swapon ]]; then
    alias swapon="/system/bin/swapon"
    alias swapoff="/system/bin/swapoff"
    alias mkswap="/system/bin/mkswap"
elif [[ -f /vendor/bin/swapon ]]; then
    alias swapon="/vendor/bin/swapon"
    alias swapoff="/vendor/bin/swapoff"
    alias mkswap="/vendor/bin/mkswap"
fi

if [[ -f /system/bin/dd ]]; then
    alias dd="/system/bin/dd"
elif [[ -f /vendor/bin/dd ]]; then
    alias dd="/vendor/bin/dd"
fi

# 读取缓冲区，调太高会增加内存占用，并且导致4K随机性能下降
read_ahead_kb=128

swap_config="/data/swap_config.conf"
function read_property()
{
    cat $swap_config | grep -v '^#' | grep "^$1=" | cut -f2 -d '='
}

# read_property xxxxx
# 解析配置
allow_swap=$(read_property swap)
config_swap_size=$(read_property swap_size)
config_swap_priority=$(read_property swap_priority)
config_swap_use_loop=$(read_property swap_use_loop)
allow_zram=$(read_property zram)
config_zram_size=$(read_property zram_size)
config_swappiness=$(read_property swappiness)
config_extra_free_kbytes=$(read_property extra_free_kbytes)
# config_auto_lmk=$(read_property auto_lmk)
config_comp_algorithm=$(read_property comp_algorithm)

swapdir="/data"
swapfile="${swapdir}/swapfile"
recreate="${swapdir}/swap_recreate"

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

function configure_swap() {
    echo "swapon swapfile"
    echo 1 > /proc/sys/vm/swap_ratio_enable
    echo 90 > /proc/sys/vm/swap_ratio

    # mkdir -p ${swapdir}

    # 判断是否需要重新创建文件
    if [[ -f ${recreate} ]]; then
        rm -rf ${swapfile}*
        rm -f ${recreate}
        echo remove swapfile
    fi
    # 是否已经创建文件
    if [[ ! -f ${swapfile} ]]; then
        dd if=/dev/zero of=${swapfile} bs=1m count=${config_swap_size}
        echo create swapfile
    fi

    # 记录挂载点
    swap_mount=$swapfile

    # 如果需要挂载为环回设备，则先挂载并记录挂载点参数
    if [[ $config_swap_use_loop ]]; then
        get_next_loop
        next_loop_index="$?"
        swap_mount="/dev/block/loop$next_loop_index"
        # losetup $swap_mount $swapfile # 挂载
        if [[ -e $swap_mount ]]
        then
            losetup -d $swap_mount      # 删除loop设备
        fi
        losetup $swap_mount $swapfile   # 挂载为loop设备
        setprop $loop_save $next_loop_index
    fi

    # 初始化
    mkswap ${swap_mount}

    # 判断是否自定义优先级
    if [[ "$config_swap_priority" != "" ]]; then
        swapon ${swap_mount} -p $config_swap_priority
    else
        swapon ${swap_mount}
    fi
}

function configure_zram() {
    current_disksize=`cat /sys/block/zram0/disksize`
    target_disksize="${config_zram_size}m" # 2001M(2098200576Bytes)
    # if [[ $current_disksize != $target_disksize ]]; then
        echo 'swapon zram0'
        swapoff /dev/block/zram0
        echo 1 > /sys/block/zram0/reset

        echo lzo > /sys/block/zram0/comp_algorithm
        # echo deflate >  /sys/block/zram0/comp_algorithm
        echo lz4 >  /sys/block/zram0/comp_algorithm
        if [[ "$config_comp_algorithm" != "" ]]; then
            echo $config_comp_algorithm /sys/block/zram0/comp_algorithm
        fi

        echo 4 > /sys/block/zram0/max_comp_streams
        echo $target_disksize > /sys/block/zram0/disksize
        mkswap /dev/block/zram0
        if [[ "$config_swap_priority" != "" ]]; then
            if [[ "$config_swap_priority" == "0" ]]; then
                swapon /dev/block/zram0 -p 0
            else
                swapon /dev/block/zram0
            fi
        else
            swapon /dev/block/zram0
        fi
    # fi
}

# Read adj series and set adj threshold for PPR and ALMK.
# This is required since adj values change from framework to framework.
adj_series=`cat /sys/module/lowmemorykiller/parameters/adj`
adj_1="${adj_series#*,}"
set_almk_ppr_adj="${adj_1%%,*}"

# PPR and ALMK should not act on HOME adj and below.
# Normalized ADJ for HOME is 6. Hence multiply by 6
# ADJ score represented as INT in LMK params, actual score can be in decimal
# Hence add 6 considering a worst case of 0.9 conversion to INT (0.9*6).
# For uLMK + Memcg, this will be set as 6 since adj is zero.
set_almk_ppr_adj=$(((set_almk_ppr_adj * 6) + 6))
echo $set_almk_ppr_adj > /sys/module/lowmemorykiller/parameters/adj_max_shift

# 50, 64, 72, 96, 128, 192MB
echo '12800,16384,18432,24576,32768,49152' > /sys/module/lowmemorykiller/parameters/minfree
echo 53059 > /sys/module/lowmemorykiller/parameters/vmpressure_file_min

# Enable adaptive LMK for all targets &
# use Google default LMK series for all 64-bit targets >=2GB.
echo 0 > /sys/module/lowmemorykiller/parameters/enable_adaptive_lmk

# Enable oom_reaper
if [ -f /sys/module/lowmemorykiller/parameters/oom_reaper ]; then
    echo 1 > /sys/module/lowmemorykiller/parameters/oom_reaper
fi

# Set allocstall_threshold to 0 for all targets.
# Set swappiness to 100 for all targets
echo 0 > /sys/module/vmpressure/parameters/allocstall_threshold
if [[ "$config_swappiness" != "" ]]; then
    # echo 65 > /proc/sys/vm/swappiness
    echo "$config_swappiness" > /proc/sys/vm/swappiness
fi

echo $read_ahead_kb /sys/block/sda/queue/read_ahead_kb

# 相比默认设置，降低了写入缓冲区大小
# 会导致IO性能下降，但能增加一点点可用RAM
echo 3 > /proc/sys/vm/dirty_background_ratio
echo 8 > /proc/sys/vm/dirty_ratio
echo 1000 > /proc/sys/vm/dirty_expire_centisecs
echo 3000 > /proc/sys/vm/dirty_writeback_centisecs

# ZRAM、SWAP
if [[ $allow_zram ]] && [[ "$config_zram_size" != "" ]]; then
    configure_zram
else
    swapoff /dev/block/zram0
fi

if [[ $allow_swap ]] && ([[ "$config_swap_size" != "" ]] || [[ -f ${swapfile} ]]); then
    configure_swap
fi


if [[ "$config_extra_free_kbytes" != "" ]]; then
    chmod 644 /proc/sys/vm/min_free_kbytes
    echo $config_extra_free_kbytes > /proc/sys/vm/min_free_kbytes

    # 128MB
    # echo 131072 > /proc/sys/vm/min_free_kbytes
    # 256MB
    # echo 262144 > /proc/sys/vm/extra_free_kbytes
fi

echo 4096 > /sys/module/process_reclaim/parameters/per_swap_size
echo 80 > /sys/module/process_reclaim/parameters/pressure_min
echo 90 > /sys/module/process_reclaim/parameters/pressure_max
echo 0 > /sys/module/process_reclaim/parameters/enable_process_reclaim
