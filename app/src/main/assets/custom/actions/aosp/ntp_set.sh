#!/system/bin/sh

if [[ "$state" == "" ]]; then
    echo '无效的操作' 1>&2
else
    settings put global ntp_server $state
    echo '好了~'
fi
