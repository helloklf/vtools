level="$1" # 清理级别（0:极微, 1：轻微，2：更重，3：极端）

# 级别0用在实时加速中，一般处于内存负载较高的状态下，此时缓存占用本就不高，无需再清理
if [[ "$level" != "0" ]]; then
  echo 3 > /proc/sys/vm/drop_caches
fi

modify_path=''
friendly=false

if [[ -f '/proc/sys/vm/extra_free_kbytes' ]]; then
  modify_path='/proc/sys/vm/extra_free_kbytes'
  friendly=true
elif [[ -f '/proc/sys/vm/min_free_kbytes' ]]; then
  modify_path='/proc/sys/vm/min_free_kbytes'
else
  echo '搞不定，你这内核不支持！'
  return 1
fi

min_free_kbytes=`getprop vtools.backup.free_kbytes`
if [[ $min_free_kbytes == '' ]]; then
  min_free_kbytes=`cat $modify_path`
  setprop vtools.backup.free_kbytes $min_free_kbytes
fi

MemTotalStr=`cat /proc/meminfo | grep MemTotal`
MemTotal=${MemTotalStr:16:8}

MemMemFreeStr=`cat /proc/meminfo | grep MemFree`
MemMemFree=${MemMemFreeStr:16:8}

SwapFreeStr=`cat /proc/meminfo | grep SwapFree`
SwapFree=${SwapFreeStr:16:8}

if [[ "$level" == "3" ]]; then
  if [[ $friendly == "true" ]]; then
    TargetRecycle=$(($MemTotal / 100 * 55))
  else
    TargetRecycle=$(($MemTotal / 100 * 26))
  fi
elif [[ "$level" == "2" ]]; then
  if [[ $friendly == "true" ]]; then
    TargetRecycle=$(($MemTotal / 100 * 35))
  else
    TargetRecycle=$(($MemTotal / 100 * 18))
  fi
elif [[ "$level" == "0" ]]; then
  if [[ $friendly == "true" ]]; then
    TargetRecycle=$(($MemTotal / 100 * 14))
  else
    TargetRecycle=$(($MemTotal / 100 * 10))
  fi
else
  if [[ $friendly == "true" ]]; then
    TargetRecycle=$(($MemTotal / 100 * 20))
  else
    TargetRecycle=$(($MemTotal / 100 * 12))
  fi
fi

zram_writback() {
  if [[ ! -f /sys/block/zram0/backing_dev ]] || [[ $(cat /proc/swaps | grep zram0) == '' ]]; then
    return 0
  fi
  backing_dev=$(cat /sys/block/zram0/backing_dev)
  if [[ "$backing_dev" != '' ]] && [[ "$backing_dev" != 'none' ]]; then
    echo all > /sys/block/zram0/idle
    echo idle > /sys/block/zram0/writeback

    MemMemFree=${MemMemFreeStr:16:8}
    if [[ $MemMemFree -gt $TargetRecycle ]]; then
      return 1
    fi
  fi
  return 0
}

force_reclaim() {
  # 计算需要回收多少内存
  RecyclingSize=$(($TargetRecycle - $MemMemFree))

  # 计算回收这些内存需要消耗的SWAP容量
  SwapRequire=$(($RecyclingSize / 100 * 130))

  # 如果没有足够的Swap容量可以回收这些内存
  # 则只拿Swap剩余容量的50%来回收内存
  if [[ $SwapFree -lt $SwapRequire ]]; then
    # 模式0优先保证性能，SWAP不足时强制回收有风险，因此不执行
    if [[ "$level" == "0" ]]; then
      echo '空闲SWAP不足以完成自动回收~'
      return 5
    fi
    RecyclingSize=$(($SwapFree / 100 * 50))
  fi

  # 最后计算出最终要回收的内存大小
  TargetRecycle=$(($RecyclingSize + $MemMemFree))

  if [[ $RecyclingSize != "" ]] && [[ $RecyclingSize -gt 0 ]]; then
    running_tag=`getprop vtools.state.force_compact`
    # 状态记录，避免同时执行多次
    if [[ "$running_tag" == "1" ]]; then
      echo '不要同时执行多次内存回收操作~'
      return 0
    else
      setprop vtools.state.force_compact 1
    fi

    echo $TargetRecycle > $modify_path
    # 级别0用在实时加速中，最重要的保持系统的持续流畅，隐藏缩短回收持续时间，减少卡顿
    if [[ "$level" == "0" ]]; then
      # TODO:去掉 log
      current_app=`getprop vtools.powercfg_app`
      echo $current_app $(($RecyclingSize / 1024))MB >> /cache/force_compact.log
      sleep_time=$(($RecyclingSize / 1024 / 120 + 2))
      if [[ $sleep_time -gt 6 ]]; then
        sleep_time=6
      fi
    else
      echo Scene App $(($RecyclingSize / 1024))MB >> /cache/force_compact.log
      sleep_time=$(($RecyclingSize / 1024 / 60 + 2))
    fi

    while [ $sleep_time -gt 0 ]; do
      sleep 1
      MemMemFreeStr=`cat /proc/meminfo | grep MemFree`
      MemMemFree=${MemMemFreeStr:16:8}

      # 如果内存已经回收足够，提前结束
      if [[ $(($TargetRecycle - $MemMemFree)) -lt 100 ]]; then
        break
      fi

      SwapFreeStr=`cat /proc/meminfo | grep SwapFree`
      SwapFree=${SwapFreeStr:16:8}
      # 如果SWAP可用空间已经不足，提前结束
      if [[ $SwapFree -lt 100 ]]; then
        break
      fi

      # 否则继续等待倒计时结束
      sleep_time=$(expr $sleep_time - 1)
    done

    # 还原原始设置
    echo $min_free_kbytes > $modify_path
    echo '好咯，内存回收结束~'

    # 清除执行状态标记
    setprop vtools.state.force_compact 0
  else
    echo '操作失败，计算容量出错!'
  fi
}

# 如果可用内存大于目标可用内存大小，则不需要回收了
if [[ $MemMemFree -gt $TargetRecycle ]]; then
  echo '内存充足，不需要操作！'
else
  zram_writback

  if [[ "$?" == '1' ]]; then
    echo '已通过ZRAM回写释放足够的内存'
  else
    force_reclaim
  fi
fi

if [[ -f /proc/sys/vm/compact_memory ]]; then
  echo 1 > /proc/sys/vm/compact_memory
fi
