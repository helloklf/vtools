#!/system/bin/sh

USER_ID='0'
if [[ -n "${ANDROID_UID}" ]]; then
    USER_ID="${ANDROID_UID}";
fi;

dir="/data/user/${USER_ID}/com.tencent.tmgp.sgame/shared_prefs"
prefs="${dir}/com.tencent.tmgp.sgame.v2.playerprefs.xml"

if [[ -f "$prefs" ]]; then
    echo 1
else
    echo 0
fi;
