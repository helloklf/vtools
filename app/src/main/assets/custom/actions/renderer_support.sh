#!/system/bin/sh

if [[ "$ANDROID_SDK" -gt 24 ]]
then
    echo 1
else
    echo "0"
fi