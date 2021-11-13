thermal_file_prefix="thermal-"
thermal_file_suffix=".conf"
# device=`getprop ro.product.device`
device=`getprop ro.product.vendor.model`

# Mi 11
if [[ $device == "M2011K2C" ]]; then
thermal_files=(
"${thermal_file_prefix}${thermal_file_suffix}"
"${thermal_file_prefix}camera${thermal_file_suffix}"
"${thermal_file_prefix}normal${thermal_file_suffix}"
"${thermal_file_prefix}per-normal${thermal_file_suffix}"
"${thermal_file_prefix}nolimits${thermal_file_suffix}"
"${thermal_file_prefix}chg-only${thermal_file_suffix}"
"${thermal_file_prefix}tgame${thermal_file_suffix}"
"${thermal_file_prefix}mgame${thermal_file_suffix}"
)

# Mi 11 Pro、11 Ultra
elif [[ $device == "M2102K1AC" ]] || [[ $device == "M2102K1C" ]]; then
thermal_file_prefix="thermal-"
thermal_file_prefix2="thermal-k1a-"
thermal_files=(
"${thermal_file_prefix}4k${thermal_file_suffix}"
"${thermal_file_prefix}8k${thermal_file_suffix}"
"${thermal_file_prefix}class0${thermal_file_suffix}"
"${thermal_file_prefix}camera${thermal_file_suffix}"
"${thermal_file_prefix}normal${thermal_file_suffix}"
"${thermal_file_prefix}per-normal${thermal_file_suffix}"
"${thermal_file_prefix}nolimits${thermal_file_suffix}"
"${thermal_file_prefix}tgame${thermal_file_suffix}"
"${thermal_file_prefix}mgame${thermal_file_suffix}"
"${thermal_file_prefix}navigation${thermal_file_suffix}"
"${thermal_file_prefix}video${thermal_file_suffix}"
"${thermal_file_prefix2}4k${thermal_file_suffix}"
"${thermal_file_prefix2}8k${thermal_file_suffix}"
"${thermal_file_prefix2}class0${thermal_file_suffix}"
"${thermal_file_prefix2}camera${thermal_file_suffix}"
"${thermal_file_prefix2}normal${thermal_file_suffix}"
"${thermal_file_prefix2}per-normal${thermal_file_suffix}"
"${thermal_file_prefix2}nolimits${thermal_file_suffix}"
"${thermal_file_prefix2}tgame${thermal_file_suffix}"
"${thermal_file_prefix2}mgame${thermal_file_suffix}"
"${thermal_file_prefix2}navigation${thermal_file_suffix}"
"${thermal_file_prefix2}video${thermal_file_suffix}"
)

# K40 Pro
elif [[ $device == "M2012K11C" ]]; then
thermal_file_prefix2="thermal-india-"
thermal_files=(
"${thermal_file_prefix}camera${thermal_file_suffix}"
"${thermal_file_prefix}class0${thermal_file_suffix}"
"${thermal_file_prefix}normal${thermal_file_suffix}"
"${thermal_file_prefix}per-normal${thermal_file_suffix}"
"${thermal_file_prefix}nolimits${thermal_file_suffix}"
"${thermal_file_prefix}tgame${thermal_file_suffix}"
"${thermal_file_prefix}mgame${thermal_file_suffix}"
"${thermal_file_prefix}navigation${thermal_file_suffix}"
"${thermal_file_prefix}video${thermal_file_suffix}"
"${thermal_file_prefix2}camera${thermal_file_suffix}"
"${thermal_file_prefix2}class0${thermal_file_suffix}"
"${thermal_file_prefix2}normal${thermal_file_suffix}"
"${thermal_file_prefix2}per-normal${thermal_file_suffix}"
"${thermal_file_prefix2}nolimits${thermal_file_suffix}"
"${thermal_file_prefix2}tgame${thermal_file_suffix}"
"${thermal_file_prefix2}mgame${thermal_file_suffix}"
"${thermal_file_prefix2}navigation${thermal_file_suffix}"
"${thermal_file_prefix2}video${thermal_file_suffix}"
)

else
thermal_files=(
"${thermal_file_prefix}4k${thermal_file_suffix}"
"${thermal_file_prefix}8k${thermal_file_suffix}"
"${thermal_file_prefix}class0${thermal_file_suffix}"
"${thermal_file_prefix}camera${thermal_file_suffix}"
"${thermal_file_prefix}normal${thermal_file_suffix}"
"${thermal_file_prefix}per-normal${thermal_file_suffix}"
"${thermal_file_prefix}nolimits${thermal_file_suffix}"
"${thermal_file_prefix}chg-only${thermal_file_suffix}"
"${thermal_file_prefix}tgame${thermal_file_suffix}"
"${thermal_file_prefix}mgame${thermal_file_suffix}"
"${thermal_file_prefix}navigation${thermal_file_suffix}"
"${thermal_file_prefix}video${thermal_file_suffix}"
)
fi