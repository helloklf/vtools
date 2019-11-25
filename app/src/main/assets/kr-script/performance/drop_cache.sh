#!/system/bin/sh

function display_mb() {
 local kbytes="$1"
 echo -n $(expr $kbytes / 1024) MB
}

# free -m
before=`cat /proc/meminfo  | grep MemAvailable | cut -F2`
echo -n '准备 '
display_mb $before
echo ''

echo -n '完成 '

sync;
echo 3 > /proc/sys/vm/drop_caches;
echo 1 > /proc/sys/vm/compact_memory;

after=`cat /proc/meminfo  | grep MemAvailable | cut -F2`
display_mb $after

echo ''
echo ''
echo ''
echo '#################'
echo ''
echo -n '结果 '

display_mb $(expr $after - $before)
echo ''

sleep 2;
