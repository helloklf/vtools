#!/system/bin/sh

source ./custom/common/magisk_replace.sh

input=./custom/common/empty
output=/system/vendor/etc/perf/perfboostsconfig.xml

if [[ $state == 0 ]]
then
    mixture_hook_file "$input" "$output" 1
else
    mixture_hook_file "$input" "$output" 0
fi