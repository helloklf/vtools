#!/system/bin/sh

input=./kr-script/common/empty
output=/system/vendor/bin/perfd

source ./kr-script/common/magisk_replace.sh

file_mixture_hooked "$input" "$output"
result="$?"

if [[ "$result" = 1 ]]
then
    echo 0
elif [[ ! -f $output ]]
then
    echo 0
else
    echo 1
fi