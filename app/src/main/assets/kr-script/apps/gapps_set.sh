#!/system/bin/sh

pm_cmd=""
if [[ "$state" = "1" ]]; then
    pm_cmd="enable"
else
    pm_cmd="disable"
fi

pm $pm_cmd com.google.android.gsf 2> /dev/null
pm $pm_cmd com.google.android.gsf.login 2> /dev/null
pm $pm_cmd com.google.android.gms 2> /dev/null
pm $pm_cmd com.android.vending 2> /dev/null
pm $pm_cmd com.google.android.play.games 2> /dev/null
pm $pm_cmd com.google.android.syncadapters.contacts 2> /dev/null
