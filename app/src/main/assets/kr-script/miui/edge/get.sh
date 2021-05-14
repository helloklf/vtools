value=`settings get system edge_size`

if [[ "$value" == "" ]] || [[ "$value" == "null" ]]; then
    echo 0
else
    awk -v x=$value -v y=100 'BEGIN{printf "%.0f\n",x*y}'
fi
