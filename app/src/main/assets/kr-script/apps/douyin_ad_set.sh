#!/system/bin/sh

function unlock_dir() {
    chattr -i $1 2> /dev/null
    $BUSYBOX chattr -i $1 2> /dev/null
    rm -rf $1 2> /dev/null
}

function lock_dir() {
    unlock_dir $1
    echo "" > $1
    chattr +i $1 2> /dev/null
    $BUSYBOX chattr +i $1 2> /dev/null
}

dir=$SDCARD_PATH/Android/data/com.ss.android.ugc.aweme
if [[ -d $dir ]]; then
    if [[ "$state" = "0" ]]; then
        lock_dir "$dir/awemeSplashCache"
        lock_dir "$dir/liveSplashCache"
        lock_dir "$dir/splashCache"
    else
        unlock_dir "$dir/awemeSplashCache"
        unlock_dir "$dir/liveSplashCache"
        unlock_dir "$dir/splashCache"
    fi
echo
    echo '没找着文件夹...'
fi