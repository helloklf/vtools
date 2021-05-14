value=`awk -v x=$level -v y=100 'BEGIN{printf "%.2f\n",x/y}'`

if [[ "$value" != "" ]]; then
    settings put system edge_type diy_suppression
    settings put system edge_size $value
fi

