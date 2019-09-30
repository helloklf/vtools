#!/system/bin/sh

# sh /data/user/0/com.omarea.vtools/files/private/scene_tools/install_busybox.sh /data/user/0/com.omarea.vtools/files/private/scene_tools

install_path="$1"

function busybox_install() {
    chmod 755 ./busybox
    for applet in `./busybox --list`; do
        ./busybox ln -sf busybox "$applet"
        chmod 755 "$applet"
    done
    ./busybox ln -sf busybox busybox_1_30_1
}

if [[ ! "$install_path" = "" ]] && [[ -d "$install_path" ]]; then
    cd "$install_path"
    if [[ ! -f busybox_1_30_1 ]]; then
        busybox_install
    fi
fi
