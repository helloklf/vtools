value=`awk -v x=$level -v y=100 'BEGIN{printf "%.2f\n",x/y}'`

if [[ "$value" != "" ]]; then
    settings put system haptic_feedback_infinite_intensity $value
fi

