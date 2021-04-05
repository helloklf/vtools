device=`getprop ro.product.vendor.model`

# Mi 11
if [[ $device == "M2011K2C" ]]; then
echo "default|系统默认(default)
performance|提高阈值(performance)
danger|丧心病狂(danger)"

# Mi 11 Pro、11 Ultra
elif [[ $device == "M2102K1AC" ]] || [[ "$device" == 'M2102K1C' ]]; then
echo "default|系统默认(default)
extreme_11pro|极致性能 (extreme)
danger|丧心病狂(danger)"
elif [ $device == "M2012K11C" ]; then
echo "default|系统默认(default)
slight_k40pro|轻微调整(slight_k40pro)
pro_k40pro|深度定制(pro_k40pro)
danger|丧心病狂(danger)"
else
echo "default|系统默认(default)
danger|丧心病狂(danger)"
fi
