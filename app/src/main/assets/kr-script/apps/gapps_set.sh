#!/system/bin/sh

function enable() {
    pm unsuspend $1 2> /dev/null
    pm enable $1 2> /dev/null
}

app_cmd="disable"
if [[ $ANDROID_SDK -gt 27 ]]; then
    app_cmd="suspend"
fi

function disable() {
    pm $app_cmd $1 2> /dev/null
}

if [ $state = '1' ]; then

    enable com.google.android.gsf
    enable com.google.android.gsf.login
    enable com.google.android.gms
    enable com.android.vending
    enable com.google.android.play.games
    enable com.google.android.syncadapters.contacts
else

    pm disable  com.google.android.gsf
    pm disable  com.google.android.gsf.login
    pm disable  com.google.android.gms
    disable com.android.vending 2> /dev/null
    disable com.google.android.play.games 2> /dev/null
    pm disable  com.google.android.syncadapters.contacts
fi;
