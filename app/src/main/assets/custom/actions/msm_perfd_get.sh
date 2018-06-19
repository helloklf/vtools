#!/system/bin/sh

path=/system/vendor/bin/perfd

echo 'perfd的作用不知道怎么解锁，总之在MIUI（9）上与上面的Performance Boost Config作用类似！'

if [[ -f $path ]]; then
    echo '当前状态：已启用'
elif [[ -f "${path}.bak" ]]; then
    echo '当前状态：已禁用'
else
    echo '当前状态：不支持'
fi
