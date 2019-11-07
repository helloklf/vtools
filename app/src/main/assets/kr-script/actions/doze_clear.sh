#!/system/bin/sh

for item in `dumpsys deviceidle whitelist`
do
    app=`echo "$item" | cut -f2 -d ','`
    echo "deviceidle whitelist -$app"
    dumpsys deviceidle whitelist -$app
done
