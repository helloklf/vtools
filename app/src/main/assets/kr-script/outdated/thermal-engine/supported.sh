if [[ -f /system/vendor/bin/thermal-engine ]] || [[ -f /system/vendor/bin/thermal-engine.bak ]]; then
    echo '1'
else
    echo '0'
fi