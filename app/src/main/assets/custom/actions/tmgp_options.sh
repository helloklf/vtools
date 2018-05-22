#!/system/bin/sh

sdk=`getprop ro.build.version.sdk`;
if [ "$sdk" -gt 23 ]; then
    echo 'VT|Vulkan-自动';
    echo 'VTF|Vulkan-强制';
fi;

echo 'O3T|OpenGLES3-自动';
echo 'O3TF|OpenGLES3-强制';
echo 'O2T|OpenGLES2';