ratio=52
min_free_kb=262144

echo 3 > /proc/sys/vm/drop_caches
echo 1 > /proc/sys/vm/compact_memory

path='/proc/sys/vm/extra_free_kbytes'

MemStr=`cat /proc/meminfo | grep MemTotal`
Mem=${MemStr:16:8}

echo $(($Mem / 100 * $ratio)) > $path

time=10

echo '等待 ' $time '秒'
sleep $time

# 还原原始设置
echo $min_free_kb > $path
echo '好咯，内存回收完毕~'
