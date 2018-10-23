#!/system/bin/sh

installed=`pm list packages | grep com.google.android.gsf`
if [[ -n "$installed" ]]
then
    echo 1
else
    echo 0
fi

