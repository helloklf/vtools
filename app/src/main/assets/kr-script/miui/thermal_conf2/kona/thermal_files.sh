#!/system/bin/sh

thermal_file_prefix="thermal-"
thermal_file_suffix=".conf"
device=`getprop ro.product.device`
model=`getprop ro.product.vendor.model`
if [[ $device == "cmi" ]] || [[ $device == "umi" ]]; then
thermal_files=(
"${thermal_file_prefix}${thermal_file_suffix}"
"${thermal_file_prefix}camera${thermal_file_suffix}"
"${thermal_file_prefix}normal${thermal_file_suffix}"
"${thermal_file_prefix}4k${thermal_file_suffix}"
"${thermal_file_prefix}notlimits${thermal_file_suffix}"
"${thermal_file_prefix}nolimits${thermal_file_suffix}"
"${thermal_file_prefix}chg-only${thermal_file_suffix}"
"${thermal_file_prefix}class0${thermal_file_suffix}"
"${thermal_file_prefix}tgame${thermal_file_suffix}"
"${thermal_file_prefix}navigation${thermal_file_suffix}"
)
elif [[ "$model" == "M2012K11AC" ]]; then
thermal_file_prefix2="thermal-india-"
thermal_files=(
"${thermal_file_prefix}camera${thermal_file_suffix}"
"${thermal_file_prefix}class0${thermal_file_suffix}"
"${thermal_file_prefix}normal${thermal_file_suffix}"
"${thermal_file_prefix}nolimits${thermal_file_suffix}"
"${thermal_file_prefix}navigation${thermal_file_suffix}"
"${thermal_file_prefix}tgame${thermal_file_suffix}"
"${thermal_file_prefix}mgame${thermal_file_suffix}"
"${thermal_file_prefix}video${thermal_file_suffix}"
"${thermal_file_prefix2}camera${thermal_file_suffix}"
"${thermal_file_prefix2}class0${thermal_file_suffix}"
"${thermal_file_prefix2}normal${thermal_file_suffix}"
"${thermal_file_prefix2}nolimits${thermal_file_suffix}"
"${thermal_file_prefix2}navigation${thermal_file_suffix}"
"${thermal_file_prefix2}tgame${thermal_file_suffix}"
"${thermal_file_prefix2}mgame${thermal_file_suffix}"
"${thermal_file_prefix2}video${thermal_file_suffix}"
)
# 10s
elif [[ "$model" == "M2102J2SC" ]]; then
thermal_file_prefix2="thermal-india-"
thermal_files=(
"${thermal_file_prefix}4k${thermal_file_suffix}"
"${thermal_file_prefix}8k${thermal_file_suffix}"
"${thermal_file_prefix}camera${thermal_file_suffix}"
"${thermal_file_prefix}class0${thermal_file_suffix}"
"${thermal_file_prefix}normal${thermal_file_suffix}"
"${thermal_file_prefix}nolimits${thermal_file_suffix}"
"${thermal_file_prefix}navigation${thermal_file_suffix}"
"${thermal_file_prefix}tgame${thermal_file_suffix}"
"${thermal_file_prefix2}camera${thermal_file_suffix}"
"${thermal_file_prefix2}normal${thermal_file_suffix}"
)
else
thermal_files=(
"${thermal_file_prefix}${thermal_file_suffix}"
"${thermal_file_prefix}camera${thermal_file_suffix}"
"${thermal_file_prefix}normal${thermal_file_suffix}"
"${thermal_file_prefix}notlimits${thermal_file_suffix}"
"${thermal_file_prefix}nolimits${thermal_file_suffix}"
"${thermal_file_prefix}chg-only${thermal_file_suffix}"
"${thermal_file_prefix}tgame${thermal_file_suffix}"
)
fi