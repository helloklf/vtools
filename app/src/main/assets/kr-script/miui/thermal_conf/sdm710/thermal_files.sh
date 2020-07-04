#!/system/bin/sh

thermal_file_prefix="thermal-engine-$platform"
thermal_file_suffix=".conf"
thermal_files=(
"${thermal_file_prefix}${thermal_file_suffix}"
"${thermal_file_prefix}-camera${thermal_file_suffix}"
"${thermal_file_prefix}-high${thermal_file_suffix}"
"${thermal_file_prefix}-notlimits${thermal_file_suffix}"
"${thermal_file_prefix}-nolimits${thermal_file_suffix}"
"${thermal_file_prefix}-phone${thermal_file_suffix}"
"${thermal_file_prefix}-pubgmhd${thermal_file_suffix}"
"${thermal_file_prefix}-sgame${thermal_file_suffix}"
"${thermal_file_prefix}-tgame${thermal_file_suffix}"
)
