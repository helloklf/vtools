#!/system/bin/sh


resource=./custom/switchs/resources/com.android.systemui
output=/system/media/theme/default/com.android.systemui

if [[ ! -f $output ]] || [[ ! -f $resource ]]
then
    echo 1
    exit 0
fi

md5=`busybox md5sum $resource | cut -f1 -d ' '`
verify=`busybox md5sum $output | cut -f1 -d ' '`

if [[ "$md5" = "$verify" ]]
then
    echo 0
else
    echo 1
fi
