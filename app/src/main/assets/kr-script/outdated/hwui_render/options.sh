#!/system/bin/sh

ANDROID_SDK=`getprop ro.build.version.sdk`

if [[ "$ANDROID_SDK" -gt 25 ]]
then
    echo 'opengl|OpenGL(Android O默认)'
    echo 'skiagl|Skia OpenGL(Android P默认)'
fi


if [[ "$ANDROID_SDK" -gt 27 ]]
then
    echo 'skiavk|Skia Vulkan'
fi
