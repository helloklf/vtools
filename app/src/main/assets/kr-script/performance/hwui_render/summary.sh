#!/system/bin/sh

render=`getprop debug.hwui.renderer`

if [[ "$render" = "opengl" ]]
then
    mode="OpenGL"
elif [[ "$render" = "skiagl" ]]
then
    mode="Skia OpenGL"
elif [[ "$render" = "skiavk" ]]
then
    mode="Skia Vulkan"
else
    mode="未知"
fi

echo "当前：$mode"