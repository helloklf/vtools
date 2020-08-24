platform=`getprop ro.board.platform | grep mt`
if [[ "$platform" != "" ]] && [[ -d /proc/ppm ]]
then
    echo 1
fi