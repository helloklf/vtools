#!/system/bin/sh

stared=`ps | grep adbd`
stared2=`ps -ef | grep adbd`
port=`getprop service.adb.tcp.port`

if [[ ! -n "$port" ]]
then
    echo 0
    return
fi

if [[ -n "$stared" ]]
then
    echo 1
elif [[ -n "$stared" ]]
then
    echo 1
else
    echo 0
fi
