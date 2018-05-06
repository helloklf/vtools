#!/system/bin/sh

text='开启OpenGLES3、Vulkan特性（可能会花屏或闪退），当前状态：'
prefs='/data/data/com.tencent.tmgp.sgame/shared_prefs/com.tencent.tmgp.sgame.v2.playerprefs.xml'
render='OpenGLES2'
mt=''

if [ `cat $prefs | grep EnableVulkan | cut -f3 -d '=' | cut -f2 -d '"'` = '2' ]; then
 render='强制Vulkan';
elif [ `cat $prefs | grep EnableVulkan | cut -f3 -d '=' | cut -f2 -d '"'` = '1' ]; then
 render='首选Vulkan';
elif [ `cat $prefs | grep EnableGLES3 | cut -f3 -d '=' | cut -f2 -d '"'` = '2' ]; then
 render='强制OpenGLES3';
elif [ `cat $prefs | grep EnableGLES3 | cut -f3 -d '=' | cut -f2 -d '"'` = '1' ]; then
 render='首选OpenGLES3';
else
 render='OpenGLES2';
fi

if [ `cat $prefs | grep EnableMTR | cut -f3 -d '=' | cut -f2 -d '"'` = '1' ]; then
    mt='多线程';
fi
if [ `cat $prefs | grep DisableMTR | cut -f3 -d '=' | cut -f2 -d '"'` = '1' ]; then
    mt='';
fi
echo $text $render $mt;
