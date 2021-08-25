#!/system/bin/sh

# TOTAL_RAM=$(awk '/^MemTotal:/{print $2}' /proc/meminfo) 2>/dev/null

function write() {
  echo -n $2 >$1
}

function round() {
  printf "%.${2}f" "${1}"
}

# 关闭当前所有的zram（如果正在使用，那可不是一般的慢）
function reset_all_zram() {
  for zram in $(blkid | grep swap | awk -F[/:] '{print $4}'); do
    zram_dev="/dev/block/${zram}"
    dev_index="$(echo $zram | grep -o "[0-9]*$")"
    write "/sys/class/zram-control/hot_remove" ${dev_index}
    swapoff ${zram_dev}
    write "/sys/block/$zram/reset" 1
    write "/sys/block/$zram/disksize" 0
  done
}

disksz_mb=768

# enable_swap_props size(Byte)
enable_swap_props() {
  local disksz=$1
  setprop vnswap.enabled true
  setprop ro.config.zram true
  setprop ro.config.zram.support true
  setprop zram.disksize ${disksz}
  write /proc/sys/vm/swappiness 100
  write /proc/sys/vm/swap_ratio_enable 1
write /proc/sys/vm/swap_ratio 70
}


# 开启zram
function enable_zram() {
  zram=""
  disksz_mb=768
  disksz=$((${disksz_mb} * 1024 * 1024))
  algorithm=lzo

  reset_all_zram

  # 获取zram序号
  if [ -e "/sys/class/zram-control/hot_add" ]; then
    RAM_DEV=$(cat /sys/class/zram-control/hot_add)
  else
     RAM_DEV='1'
  fi

  if [ -z ${zram} ]; then
    zram="zram${RAM_DEV}"
    zram_dev="/dev/block/${zram}"
  fi

  swapoff ${zram_dev} >/dev/null 2>&1
  write /sys/block/${zram}/comp_algorithm ${algorithm} # 压缩算法
  # write /sys/block/${zram}/max_comp_streams 8
  write /sys/block/${zram}/reset 1
  write /sys/block/${zram}/disksize ${disksz_mb}M
  mkswap ${zram_dev} >/dev/null 2>&1
  swapon ${zram_dev} >/dev/null 2>&1

  # enable_swap_props
}

# 禁用zram
function disable_zram() {
  reset_all_zram

  setprop vnswap.enabled false
  setprop ro.config.zram false
  setprop ro.config.zram.support false
  setprop zram.disksize 0
  write /proc/sys/vm/swappiness 0
}

