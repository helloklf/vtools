settings put secure ui_night_mode $state
echo '这得看人品，不一定能切换成功'
if [[ $hotreboot = "1" ]]; then
  echo '3秒后自动重启'
  sync
  sleep 3
  # busybox killall system_server
  reboot
else
  echo '部分系统可能需要重启手机才会生效'
fi