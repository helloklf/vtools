#!/system/bin/sh

source ./kr-script/common/magisk_replace.sh

input=./kr-script/common/empty
output=/system/vendor/bin/perfd

stop perfd
if [[ $state == 0 ]]
then
    mixture_hook_file "$input" "$output" 1
else
    mixture_hook_file "$input" "$output" 0
fi
