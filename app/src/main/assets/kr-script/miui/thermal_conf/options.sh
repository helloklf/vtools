soc_platform=`getprop ro.board.platform`
resource_dir="./kr-script/miui/thermal_conf/$soc_platform/"

if [[ -f $resource_dir/options.sh ]]; then
    . $resource_dir/options.sh
elif [[ -f $resource_dir/options.txt ]]; then
    cat $resource_dir/options.txt
fi