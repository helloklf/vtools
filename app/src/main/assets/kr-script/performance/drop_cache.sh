#!/system/bin/sh

function display_mb() {
 local kbytes="$1"
 echo -n $(expr $kbytes / 1024) MB
}

# free -m
before=`cat /proc/meminfo  | grep MemAvailable | cut -F2`
echo -n '清理之前： '
display_mb $before
echo ''

echo -n '清理之后： '

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
echo -n '回收内存： '

display_mb $(expr $after - $before)
echo ''

sleep 2;
