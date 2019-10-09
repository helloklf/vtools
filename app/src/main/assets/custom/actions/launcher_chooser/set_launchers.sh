#!/system/bin/sh

# pm query-activities -a android.intent.action.MAIN -c android.intent.category.HOME | grep name=
# pm query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME
# pm set-home-activity com.google.android.apps.nexuslauncher/.NexusLauncherActivity  --user 0

# 终止所有可能在运行的桌面程序
launchers=$(pm query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME | grep '/')
for launcher in $launchers ; do
    packageName=`echo $launcher | cut -f1 -d '/'`
    am force-stop $packageName 2>/dev/null
    killall -9 $packageName 2>/dev/null
done

# 切换桌面
activity="$state"
if [[ "$activity" != "" ]]; then
    echo "切换桌面为[$activity]"
    pm set-home-activity $activity --user ${ANDROID_UID}
fi

# 模拟home键 返回桌面
input keyevent 3