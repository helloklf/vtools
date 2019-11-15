#!/system/bin/sh

path=/system/vendor/etc/perf/perfboostsconfig.xml

if [[ -f $path ]]; then
    echo 1
elif [[ -f "${path}.bak" ]]; then
    echo 1
else
    echo 0
fi
