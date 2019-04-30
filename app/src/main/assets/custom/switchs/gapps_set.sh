#!/system/bin/sh

if [ $state = '1' ]; then
    pm enable com.google.android.gsf
    pm enable com.google.android.gsf.login
    pm enable com.google.android.gms
    pm enable com.android.vending
    pm enable com.google.android.play.games 2> /dev/null
else
    pm disable com.google.android.gsf
    pm disable com.google.android.gsf.login
    pm disable com.google.android.gms
    pm disable com.android.vending
    pm disable com.google.android.play.games 2> /dev/null
fi;
