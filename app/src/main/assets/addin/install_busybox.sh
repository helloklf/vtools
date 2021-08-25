#!/system/bin/sh

install_path="$1"

busybox_install() {
  chmod 755 ./busybox
  for applet in `./busybox --list`; do
    case "$applet" in
    "sh"|"busybox"|"shell"|"swapon"|"swapoff"|"mkswap")
      echo 'Skip' > /dev/null
    ;;
    *)
      ./busybox ln -sf busybox "$applet"
      chmod 755 "$applet"
    ;;
    esac
  done
  ./busybox ln -sf busybox busybox_1_30_1
}

if [[ ! "$install_path" = "" ]] && [[ -d "$install_path" ]]; then
  cd "$install_path"
  if [[ ! -f busybox_1_30_1 ]]; then
    busybox_install
  fi
fi
