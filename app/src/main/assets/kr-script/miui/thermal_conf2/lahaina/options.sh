device=`getprop ro.product.vendor.model`

# Mi 11
if [[ $device == "M2011K2C" ]]; then
echo "default|系统默认(default)
cool_11|清爽酷凉(cool)
pro_11|深度定制(pro)
extreme_11|极致性能(extreme)
danger_11|丧心病狂(danger)"

# Mi 11 Pro、Ultra
elif [[ $device == "M2102K1AC" ]] || [[ "$device" == 'M2102K1C' ]]; then
echo "default|系统默认(default)
cool_11pro|清爽酷凉(cool)
pro_11pro|深度定制(pro)
extreme_11pro|极致性能(extreme)
danger_11pro|丧心病狂(danger)"

# K40 Pro
elif [ $device == "M2012K11C" ]; then
echo "default|系统默认(default)
slight_k40pro|轻微调整(slight)
pro_k40pro|深度定制(pro)
danger_k40pro|丧心病狂(danger)"

else
echo "default|系统默认(default)
danger|丧心病狂(danger)"
fi
