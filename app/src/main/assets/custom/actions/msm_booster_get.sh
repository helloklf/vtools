#!/system/bin/sh

path=/system/vendor/etc/perf/perfboostsconfig.xml

echo 'Performance Boost Config是系统内置的一种调节机制，在启动应用或打开活动时短时间内将CPU提速到最大频率。\n如果你需要使用内核调谐器手动控制设备性能，请先禁用它！'

if [[ -f $path ]]; then
    echo '当前状态：已启用'
elif [[ -f "${path}.bak" ]]; then
    echo '当前状态：已禁用'
else
    echo '当前状态：不支持'
fi
