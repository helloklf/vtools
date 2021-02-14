device=`getprop ro.product.vendor.model`

if [[ $device == "M2011K2C" ]]; then
echo "default|系统默认(default)
performance|提高阈值 (performance)
danger|丧心病狂(danger)"
else
echo "default|系统默认(default)
danger|丧心病狂(danger)"
fi
