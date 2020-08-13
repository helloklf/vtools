#!/system/bin/sh

pm_cmd=""
if [[ "$state" = "1" ]]; then
    pm_cmd="enable"
    echo '√√  启用  Google服务套件'
else
    pm_cmd="disable"
    echo '××  停用  Google服务套件'
fi

echo ''
echo ''

pm $pm_cmd com.google.android.gsf 2> /dev/null
pm $pm_cmd com.google.android.gsf.login 2> /dev/null
pm $pm_cmd com.google.android.gms 2> /dev/null
pm $pm_cmd com.android.vending 2> /dev/null
pm $pm_cmd com.google.android.play.games 2> /dev/null
pm $pm_cmd com.google.android.syncadapters.contacts 2> /dev/null
