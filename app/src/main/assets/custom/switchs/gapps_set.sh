#!/system/bin/sh

if [ $state = '1' ]; then
    pm enable com.google.android.gsf 2> /dev/null
    pm enable com.google.android.gsf.login 2> /dev/null
    pm enable com.google.android.gms 2> /dev/null
    pm enable com.android.vending 2> /dev/null
    pm enable com.google.android.play.games 2> /dev/null
else
    pm disable com.google.android.gsf 2> /dev/null
    pm disable com.google.android.gsf.login 2> /dev/null
    pm disable com.google.android.gms 2> /dev/null
    pm disable com.android.vending 2> /dev/null
    pm disable com.google.android.play.games 2> /dev/null
fi;
