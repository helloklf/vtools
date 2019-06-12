#!/system/bin/sh

input=./custom/common/empty
output=/system/vendor/etc/perf/perfboostsconfig.xml

source ./custom/common/magisk_replace.sh

file_mixture_hooked "$input" "$output"
result="$?"

if [[ "$result" = 1 ]]
then
    echo 0
else
    echo 1
fi