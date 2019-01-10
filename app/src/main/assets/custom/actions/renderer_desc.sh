#!/system/bin/sh

render=`getprop debug.hwui.renderer`
echo "切换HWUI渲染引擎（Skia Vulkan 仅支持Android P及更高版本！），当前："

if [[ "$render" = "opengl" ]]
then
    echo "OpenGL"
elif [[ "$render" = "skiagl" ]]
then
    echo "Skia OpenGL"
elif [[ "$render" = "skiavk" ]]
then
    echo "Skia Vulkan"
else
    echo "未知"
fi