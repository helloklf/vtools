#!/system/bin/sh

if [[ "$level" = "null" ]]
then
    settings reset global low_power_trigger_level
else
    settings put global low_power_trigger_level $level
fi

if [[ "$levelmax" = "null" ]]
then
    settings reset global low_power_trigger_level_max
else
    settings put global low_power_trigger_level_max $levelmax
fi
