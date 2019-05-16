#!/system/bin/sh

path="${MAGISK_PATH}/system/vendor/etc/thermal-engine.current.ini"
if [[ -f "${MAGISK_PATH}/system/vendor/etc/thermal-engine.current.ini" ]]
then
    cat $path
else
    echo ''
fi