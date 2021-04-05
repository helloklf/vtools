device=`getprop ro.product.vendor.model`

if [[ "$device" == 'M2011K2C' ]] || [[ "$device" == 'M2102K1AC' ]] || [[ "$device" == 'M2102K1C' ]] || [[ "$device" == 'M2012K11C' ]]; then
    echo 1
else
    echo 0
fi