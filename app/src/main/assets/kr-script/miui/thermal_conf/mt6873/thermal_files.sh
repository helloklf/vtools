#!/system/bin/sh

thermal_file_prefix="thermal-$platform"
thermal_file_suffix=".conf"
thermal_files=(
"${thermal_file_prefix}-chg-only${thermal_file_suffix}"
"${thermal_file_prefix}-normal${thermal_file_suffix}"
"${thermal_file_prefix}-notlimits${thermal_file_suffix}"
"${thermal_file_prefix}-tgame${thermal_file_suffix}"
)
