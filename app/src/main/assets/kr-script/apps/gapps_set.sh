#!/system/bin/sh

apps="
com.google.android.gsf
com.google.android.gsf.login
com.google.android.gms
com.android.vending
com.google.android.play.games
com.google.android.syncadapters.contacts
"
if [ $state = '1' ]; then
    for app in $apps; do
        pm enable $app 2> /dev/null
        pm unsuspend $app 2> /dev/null
    done
else
    for app in $apps; do
        pm suspend $app 2> /dev/null || pm disable $app 2> /dev/null
    done
fi;
