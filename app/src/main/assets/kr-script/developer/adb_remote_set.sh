#!/system/bin/sh

if [[ ! "$state" = "1" ]]
then
    setprop service.adb.tcp.port -1
    stop adbd
    killall -9 adbd 2>/dev/null
    start adbd
    echo '远程调试服务已停止'
    setprop service.adb.tcp.port ""
    return 0
fi

setprop service.adb.tcp.port 5555;
stop adbd;
killall -9 adbd 2>/dev/null
sleep 1;
start adbd;

ip=`ifconfig wlan0 | grep "inet addr" | awk '{ print $2}' | awk -F: '{print $2}'` 2>/dev/null
if [[ ! -n "$ip" ]]
then
    ip=`ifconfig eth0 | grep "inet addr" | awk '{ print $2}' | awk -F: '{print $2}'` 2>/dev/null
fi

echo "在连接了和手机相同网络(局域网)的电脑上"
echo '通过下面的命令'
echo ''

if [[ -n "$ip" ]]
then
    echo "adb connect $ip:5555"
else
    echo "adb connect 手机IP:5555"
fi

echo '来连接手机'
