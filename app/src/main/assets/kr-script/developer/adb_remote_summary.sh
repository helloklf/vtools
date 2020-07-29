#!/system/bin/sh

stared=`ps | grep adbd`
stared2=`ps -ef | grep adbd`
port=`getprop service.adb.tcp.port`

if [[ ! -n "$port" ]]
then
    echo ""
    return
fi


ip=`ifconfig wlan0 | grep "inet addr" | awk '{ print $2}' | awk -F: '{print $2}'` 2>/dev/null
if [[ ! -n "$ip" ]]
then
    ip=`ifconfig eth0 | grep "inet addr" | awk '{ print $2}' | awk -F: '{print $2}'` 2>/dev/null
fi

if [[ -n "$stared" ]] || [[ -n "$stared2" ]]; then
    if [[ -n "$ip" ]]
    then
        echo "$ip:$port"
    else
        echo "手机IP:$port"
    fi
else
    echo ""
fi
