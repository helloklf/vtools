#!/system/bin/sh

for sensor in /sys/class/thermal/*
do
  if [[ -f $sensor/temp ]]
  then
    echo `cat $sensor/type`   `cat $sensor/temp`
  fi
done

