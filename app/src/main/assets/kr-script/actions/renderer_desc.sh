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

echo "切换HWUI渲染引擎（Skia Vulkan 仅支持Android P及更高版本！），当前：$mode"