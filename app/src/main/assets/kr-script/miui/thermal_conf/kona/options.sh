device=`getprop ro.product.device`

# UMI = Mi 10
# CMI = Mi 10Prp
# LMI = Redmi K30Pro（普通版+变焦版）

if [[ $device == "cmi" ]] || [[ $device == "umi" ]]; then
echo "default|系统默认(default)
powerfrugal_cmi|节能降温(powerfrugal)
extreme_cmi|极致性能(extreme)
danger|丧心病狂(danger)"
elif [[ $device == "lmi" ]]; then
echo "default|系统默认(default)
extreme|极致性能(extreme)
danger|丧心病狂(danger)"
else
echo "default|系统默认(default)
danger|丧心病狂(danger)"
fi
