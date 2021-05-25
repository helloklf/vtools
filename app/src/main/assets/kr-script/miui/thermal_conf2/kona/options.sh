device=`getprop ro.product.device`
model=`getprop ro.product.vendor.model`

# UMI = Mi 10
# CMI = Mi 10Prp
# LMI = Redmi K30Pro（普通版+变焦版）

if [[ $device == "cmi" ]]; then
echo "default|系统默认(default)
cool_cmi|清爽酷凉(cool)
powerfrugal_cmi|节能降温(powerfrugal)
pro_cmi|深度定制(pro)
extreme_cmi|极致性能(extreme)
danger|丧心病狂(danger)"

elif [[ $device == "umi" ]]; then
echo "default|系统默认(default)
cool_umi|清爽酷凉(cool)
pro_umi|深度定制(pro)
danger|丧心病狂(danger)"

elif [[ $device == "lmi" ]] || [[ $device == "lmipro" ]]; then
echo "default|系统默认(default)
extreme|极致性能(extreme)
danger|丧心病狂(danger)"

elif [[ "$model" == "M2012K11AC" ]]; then
echo "default|系统默认(default)
slight_k40|轻微调整(slight)
danger|丧心病狂(danger)"

elif [[ "$model" == "M2102J2SC" ]]; then
echo "default|系统默认(default)
pro_10s|深度定制(pro)
slight_10s|轻微调整(slight)
danger|丧心病狂(danger)"

else
echo "default|系统默认(default)
danger|丧心病狂(danger)"
fi
