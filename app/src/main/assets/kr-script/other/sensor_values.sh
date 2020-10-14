#!/system/bin/sh

# for sensor in /sys/class/thermal/*

cd /sys/class/thermal/
for sensor in *
do
  if [[ -f $sensor/temp ]]
  then
    echo `cat $sensor/type` `cat $sensor/temp` # $sensor
  fi
done

