value=`settings get system haptic_feedback_infinite_intensity`

if [[ "$value" == "" ]] || [[ "$value" == "null" ]]; then
    echo 0
else
    awk -v x=$value -v y=100 'BEGIN{printf "%.0f\n",x*y}'
fi
