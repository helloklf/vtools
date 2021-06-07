prop="persist.vtools.suspend"
status=`getprop $prop`

function on() {
apps=`pm list package -3 | grep -v com.omarea | grep -v launcher | grep -v xposed | grep -v magisk | cut -f2 -d ':'`
system_apps="
com.xiaomi.market
com.miui.player
com.miui.video
com.xiaomi.ab
com.miui.gallery
com.android.fileexplorer
com.android.browser
com.google.android.gsf
com.google.android.gsf.login
com.google.android.gms
com.android.vending
com.google.android.play.games
com.google.android.syncadapters.contacts
"

    echo '进入待机模式'
    echo ''
    echo '此过程可能需要 10~60 秒'
    echo ''
    echo '冻结所有第三方应用'
    echo ''

    for app in $apps; do
      am force-stop $app 1 > /dev/null
      pm suspend $app 1 > /dev/null
    done
    for app in $system_apps; do
      am force-stop $app 1 > /dev/null
      pm suspend $app 1 > /dev/null
    done

    setprop $prop 1

    svc wifi disable
    svc data disable

    settings put global low_power 1;

    echo "开启应用自动限制 可能需要Android Pie"
    settings put global app_auto_restriction_enabled true

    echo "开启应用强制standby"
    settings put global forced_app_standby_enabled 1

    echo "开启应用standby"
    settings put global app_standby_enabled 1

    echo "开启安卓原生的省电模式"
    settings put global low_power 1

    sync

    echo 3 > /proc/sys/vm/drop_caches

    # 电源键 息屏
    input keyevent 26
    sleep 5

    echo "进入闲置状态"
    dumpsys deviceidle step
    dumpsys deviceidle step
    dumpsys deviceidle step
    dumpsys deviceidle step
}

function off() {
    echo '退出待机模式'
    echo '此过程可能需要 10~60 秒'
    echo ''

    for app in `pm list package | cut -f2 -d ':'`; do
      pm unsuspend $app 1 > /dev/null
    done

    # svc wifi enable
    # svc data enable

    settings put global low_power 0;

    echo "关闭应用自动限制 可能需要Android Pie"
    settings put global app_auto_restriction_enabled false

    echo "关闭应用强制standby"
    settings put global forced_app_standby_enabled 0

    echo "开启应用standby"
    settings put global app_standby_enabled 1

    echo "关闭安卓原生的省电模式"
    settings put global low_power 0

    setprop $prop 0
}

if [[ "$status" = "1" ]]; then
    off
else
    on
fi
