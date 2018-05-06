#!/system/bin/sh

sdk=`getprop ro.build.version.sdk`;
if [ $sdk -gt 23 ]; then
    echo 'VT|Vulkan多线程-自动';
    echo 'VTF|Vulkan多线程-强制';
fi;

echo 'O3T|OpenGLES3多线程-自动';
echo 'O3TF|OpenGLES3多线程-强制';
echo 'O2T|OpenGLES2多线程';