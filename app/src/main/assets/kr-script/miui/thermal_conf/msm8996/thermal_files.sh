#!/system/bin/sh

thermal_file_prefix="thermal-engine-8996"
thermal_file_suffix=".conf"
thermal_files=(
"${thermal_file_prefix}${thermal_file_suffix}"
"${thermal_file_prefix}-high${thermal_file_suffix}"
"${thermal_file_prefix}-game${thermal_file_suffix}"
"${thermal_file_prefix}-map${thermal_file_suffix}"
)
