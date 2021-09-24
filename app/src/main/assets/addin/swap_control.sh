swapfile=/data/swapfile
action="$1"     # 操作(脚本中的函数名)
use_loop="$2"       # 是否挂载为loop设备
priority="$3"   # 优先级
swapsize="$4"   # SWAP大小MB

loop_save="vtools.swap.loop"
next_loop_path=""

# 获取下一个可用的loop设备
get_next_loop() {
  local current_loop=`getprop $loop_save`

  if [[ "$current_loop" != "" ]]; then
    next_loop_path="$current_loop"
    return
  fi

  loop_str=$(losetup -f >/dev/null 2>&1)
  if [[ "$loop_str" == "" ]] && [[ $(busybox losetup -f 2>/dev/null) != "" ]]; then
    alias losetup='busybox losetup'
  fi

  nl=$(losetup -f 2>/dev/null | egrep -o '[0-9]{1,}')
  if [[ "$nl" != "" ]]; then
    next_loop_path="/dev/block/loop$nl"
    return
  fi

  local loop_index=0
  local used=`blkid | grep /dev/block/loop`
  for loop in /dev/block/loop*
  do
    if [[ "$loop_index" -gt "0" ]]; then
      if [[ `echo $used | grep /dev/block/loop$loop_index` = "" ]]; then
        next_loop_path="/dev/block/loop$loop_index"
        return
      fi
    fi
    local loop_index=`expr $loop_index + 1`
  done

  next_loop_path=""
}

if [[ "$use_loop" == "1" ]]; then
  get_next_loop
  if [[ "$next_loop_path" != "" ]]; then
    swap_mount=$next_loop_path
  else
    echo '所有回环设备都已被占用，无法完成挂载！'
    return
  fi
else
  swap_mount=$swapfile
fi

# 关闭swap（如果正在使用，那可不是一般的慢）
disable_swap() {
  swapoff $swap_mount >/dev/null 2>&1
  if [[ $use_loop == "1" ]]; then
    losetup -d $swap_mount >/dev/null 2>&1
  fi
  setprop $loop_save ""
}

# 开启SWAP
enable_swap() {
  if [[ ! -f $swapfile ]]; then
    if [[ "$swapsize" = "" ]]; then
      swapsize=256
    fi
    dd if=/dev/zero of=$swapfile bs=1048576 count=$swapsize # 创建
  fi

  if [[ "$use_loop" == "1" ]]; then
    # losetup $swap_mount $swapfile # 挂载
    if [[ -e $swap_mount ]]; then
      losetup -d $swap_mount 2>/dev/null      # 删除loop设备
    fi
    losetup $swap_mount $swapfile   # 挂载为loop设备
    setprop $loop_save $next_loop_path
  fi

  mkswap $swap_mount | grep -v UUID 2>&1 # 初始化
  if [[ "$priority" != "" ]]; then
    # zram_priority=`cat /proc/swaps | grep /zram0 | sed 's/[ \t][ ]*/,/g' | cut -f5 -d ','`
    zram_info=`cat /proc/swaps | grep /zram0`
    if [[ $zram_info != "" ]]; then
      zram_priority=`echo $zram_info | sed 's/[ \t][ ]*/,/g' | cut -f5 -d ','`
    fi

    if [[ "$zram_priority" != "" ]]; then
      if [[ "$priority" == '0' ]]; then
        if [[ "$zram_priority" -lt 0 ]]; then
          if [[ -f /sys/block/zram0/backing_dev ]]; then
            backing_dev=$(cat /sys/block/zram0/backing_dev)
          fi
          swapoff /dev/block/zram0
          if [[ -f /sys/block/zram0/backing_dev ]]; then
            echo $backing_dev > /sys/block/zram0/backing_dev
          fi
          swapon /dev/block/zram0 -p $priority 2>&1
        else
          priority="$zram_priority"
        fi
      elif [[ "$priority" == '5' ]] && [[ "$zram_priority" -gt 5 ]]; then
        priority="32767"
      fi
    fi

    if [[ "$priority" -lt 0 ]]; then
      swapon $swap_mount 2>&1
    else
      swapon $swap_mount -p "$priority" 2>&1
    fi
  else
    swapon $swap_mount 2>&1
  fi

  echo 100 > /proc/sys/vm/overcommit_ratio
  if [[ -f /proc/sys/vm/swap_ratio ]]; then
    echo 100 > /proc/sys/vm/swap_ratio
  fi
  if [[ -f /proc/sys/panic_on_oom ]]; then
   echo 0 > /proc/sys/panic_on_oom
  fi
}

"$action"
