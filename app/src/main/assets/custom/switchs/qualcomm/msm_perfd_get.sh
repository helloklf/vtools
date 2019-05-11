#!/system/bin/sh

path=/system/vendor/bin/perfd

if [[ -f $path ]]; then
    echo 1
elif [[ -f "${path}.bak" ]]; then
    echo 0
else
    # echo '当前状态：不支持'
    echo 0
fi
