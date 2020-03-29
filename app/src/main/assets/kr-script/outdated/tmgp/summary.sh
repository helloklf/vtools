#!/system/bin/sh

text='当前：'

USER_ID='0'
if [[ -n "${ANDROID_UID}" ]]; then
    USER_ID="${ANDROID_UID}";
fi;

dir="/data/user/${USER_ID}/com.tencent.tmgp.sgame/shared_prefs"
prefs="${dir}/com.tencent.tmgp.sgame.v2.playerprefs.xml"

if [[ ! -f "$prefs" ]]; then
    echo $text "配置不存在";
    exit 0;
fi;

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
    mt=''
fi

echo $text $render $mt;
