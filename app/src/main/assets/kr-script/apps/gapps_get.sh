#!/system/bin/sh

disabled=`pm list packages -d | grep com.google.android.gsf`
if [[ -n "$disabled" ]]
then
    echo 0
else
    echo 1
fi

