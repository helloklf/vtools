#!/system/bin/sh

function display_mb() {
 local kbytes="$1"
 echo -n $(expr $kbytes / 1024) MB
}

# free -m
before=`cat /proc/meminfo  | grep MemAvailable | cut -F2`
echo '可用内存  '
display_mb $before

sync;
echo 3 > /proc/sys/vm/drop_caches;
echo 1 > /proc/sys/vm/compact_memory;

after=`cat /proc/meminfo  | grep MemAvailable | cut -F2`

echo -n ' > '
display_mb $after

echo ''
echo ''
echo '#################'
echo ''
echo '回收了    '
echo ''

display_mb $(expr $before - $after)
echo ''

sleep 2;
