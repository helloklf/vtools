#!/system/bin/sh

render=`getprop debug.hwui.renderer`

if [[ "$render" = "opengl" ]] || [[ "$render" = "skiagl" ]] || [[ "$render" = "skiavk" ]]
then
    echo 1
else
    echo "0"
fi