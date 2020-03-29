#!/system/bin/sh

sdk=`getprop ro.build.version.sdk`;
if [ "$sdk" -gt 23 ]; then
    echo 'VT|优先Vulkan';
    echo 'VTF|强制Vulkan';
fi;

echo 'O3T|优先OpenGLES3';
echo 'O3TF|强制OpenGLES3';
echo 'O2T|OpenGLES2';