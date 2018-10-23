#!/system/bin/sh

if [[ ! "$1" = "1" ]]
then
    stop adbd
    echo '远程调试服务已停止'
    return 0
fi

setprop service.adb.tcp.port 5555;
stop adbd;
sleep 1;
start adbd;

ip=`ifconfig wlan0 | grep "inet addr" | awk '{ print $2}' | awk -F: '{print $2}'`
if [[ ! -n "$ip" ]]
then
    ip=`ifconfig eth0 | grep "inet addr" | awk '{ print $2}' | awk -F: '{print $2}'`
fi

echo "在和手机连接相同局域网的电脑上"
echo "通过请通过手机adb connect 手机IP:5555连接设备"

if [[ -n "$ip" ]]
then
    echo ''
    echo "当前设备WIFI IP："
    echo "$ip"
fi
