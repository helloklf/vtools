version=`getprop ro.build.version.sdk`

if [[ "$version" > 28 ]]; then
    settings put secure location_mode 0
elif [[ "$version" > 22 ]]; then
    settings put secure location_providers_allowed -gps
else
    settings put secure location_providers_allowed network
fi
