governor_backup () {
  local governor_backup=/cache/governor_backup.prop
  if [[ ! -f $governor_backup ]]; then
    echo '' > $governor_backup
    local dir=/sys/class/devfreq
    for file in `ls $dir`; do
      if [ -f $dir/$file/governor ]; then
        governor=`cat $dir/$file/governor`
        echo "$file#$governor" >> $governor_backup
      fi
    done
  fi
}

governor_performance () {
  governor_backup
  local dir=/sys/class/devfreq
  for file in `ls $dir`; do
    if [ -f $dir/$file/governor ]; then
      echo $dir/$file/governor
      echo performance > $dir/$file/governor
    fi
  done
}

governor_restore () {
  local governor_backup=/cache/governor_backup.prop
  local dir=/sys/class/devfreq
  if [[ -f "$governor_backup" ]]; then
      while read line; do
        if [[ "$line" != "" ]]; then
            echo ${line#*#} > $dir/${line%#*}/governor
        fi
      done < /cache/governor_backup.prop
  fi
}

if [[ "$action" == "fast" ]]; then
  governor_performance
else
  governor_restore
fi