#!/system/bin/sh

# total_len=0
#
# for file in `find /vendor -name thermal* -type f`; do
#     total_len=`expr $total_len + 1`
# done
# for file in `find /system -name thermal* -type f`; do
#     total_len=`expr $total_len + 1`
# done
#
# # echo $total_len
#
# echo "view|查看找到的温控文件"
# echo "delete|删除${total_len}个名字带thermal的文件"
# echo "replace|用Magisk屏蔽${total_len}个名字带thermal的文件"
# echo "cancel|取消通过Magisk屏蔽的温控文件"

echo "view|查看找到的温控文件"
echo "delete|删除名称包含thermal的文件"