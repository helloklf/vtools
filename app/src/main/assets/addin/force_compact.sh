level="$1" # 清理级别（1：轻微，2：更重，3：极端）

echo 3 > /proc/sys/vm/drop_caches

modify_path=''
friendly=false

if [[ -f '/proc/sys/vm/extra_free_kbytes' ]]; then
  modify_path='/proc/sys/vm/extra_free_kbytes'
  friendly=true
elif [[ -f '/proc/sys/vm/min_free_kbytes' ]]; then
  modify_path='/proc/sys/vm/min_free_kbytes'
else
  echo '搞不定，你这内核不支持！'
  exit 1
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
    TargetRecycle=$(($MemTotal / 100 * 45))
  else
    TargetRecycle=$(($MemTotal / 100 * 18))
  fi
else
  if [[ $friendly == "true" ]]; then
    TargetRecycle=$(($MemTotal / 100 * 25))
  else
    TargetRecycle=$(($MemTotal / 100 * 10))
  fi
fi

# 如果可用内存大于目标可用内存大小，则不需要回收了
if [[ $MemMemFree -gt $TargetRecycle ]]; then
  echo '内存充足，不需要操作！'
else
  # 计算需要回收多少内存
  RecyclingSize=$(($TargetRecycle - $MemMemFree))

  # 计算回收这些内存需要消耗的SWAP容量
  SwapRequire=$(($RecyclingSize / 100 * 130))

  # 如果没有足够的Swap容量可以回收这些内存
  # 则只拿Swap剩余容量的60%来回收内存
  if [[ $SwapFree -lt $SwapRequire ]]; then
      RecyclingSize=$(($SwapFree / 100 * 60))
  fi

  # 最后计算出最终要回收的内存大小
  TargetRecycle=$(($RecyclingSize + $MemMemFree))

  if [[ $RecyclingSize != "" ]] && [[ $RecyclingSize -gt 0 ]]; then
    running_tag=`getprop vtools.state.force_compact`
    # 状态记录，避免同时执行多次
    if [[ "$running_tag" == "1" ]]; then
      echo '不要同时执行多次内存回收操作~'
      exit 0
    else
      setprop vtools.state.force_compact 1
    fi

    echo $TargetRecycle > $modify_path
    sleep_time=$(($RecyclingSize / 1024 / 60 + 2))

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
fi
echo 1 > /proc/sys/vm/compact_memory
