value=`settings get system edge_size`

if [[ "$value" == "" ]] || [[ "$value" == "null" ]]; then
    echo ""
else
    value=`awk -v x=$value -v y=100 'BEGIN{printf "%.0f\n",x*y}'`
    echo "当前: ${value}%"
fi
