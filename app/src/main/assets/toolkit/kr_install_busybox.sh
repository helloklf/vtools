#!/system/bin/sh

function busybox_install() {
    for applet in `./busybox --list`; do
        ./busybox ln -sf busybox "$applet";
    done
    ./busybox ln -sf busybox busybox_1_30_1
}

if [[ ! "$TOOLKIT" = "" ]]; then
    cd "$TOOLKIT"
    if [[ ! -f busybox_1_30_1 ]]; then
        busybox_install
    fi
fi
