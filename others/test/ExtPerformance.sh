governor_performance () {
  local dir=/sys/class/devfreq
  for file in `ls $dir`; do
    if [ -f $dir/$file/available_frequencies ]; then
      max_freq=$(awk -F ' ' '{print $NF}' $dir/$file/available_frequencies)
      if [[ "$max_freq" != "" ]]; then
        echo $file '->' $max_freq
        echo $max_freq > $dir/$file/max_freq
        echo $max_freq > $dir/$file/min_freq
      fi
    fi
  done
}

governor_restore () {
  local dir=/sys/class/devfreq
  for file in `ls $dir`; do
    if [ -f $dir/$file/available_frequencies ]; then
      min_freq=$(awk '{print $1}' $dir/$file/available_frequencies)
      if [[ "$min_freq" != "" ]]; then
        echo $file '->' $min_freq
        echo $min_freq > $dir/$file/min_freq
      fi
    fi
  done
}

if [[ "$action" == "fast" ]] || [[ "$action" == "performance" ]]; then
  governor_performance
else
  governor_restore
fi