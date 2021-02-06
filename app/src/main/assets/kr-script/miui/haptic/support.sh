if [[ `getprop sys.haptic.motor` == 'linear' ]]; then
    value=`settings get system haptic_feedback_infinite_intensity`
    if [[ "$value" == "" ]] || [[ "$value" == "null" ]]; then
        echo 0
    else
        echo 1
    fi
else
    echo 0
fi
