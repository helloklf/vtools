echo '此操作并不会提升性能' 1>&2
echo '如果你不知道是什么，请点界面上的 [退出] 按钮！' 1>&2
echo ''
echo '否则，25秒后开始执行操作' 1>&2
echo '下次开机需要很长时间…' 1>&2

echo ''
sleep 25
sync
echo ''
rm -rf /cache/dalvik-cache
reboot