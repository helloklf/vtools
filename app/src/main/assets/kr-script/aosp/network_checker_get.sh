mode=$(settings get global captive_portal_mode)
if [[ "$mode" == "0" ]]; then
  echo 'disable'
else
  url=$(settings get global captive_portal_https_url)
  case "$url" in
    *"google"*)
      echo 'google'
    ;;
    *"miui"*)
      echo 'miui'
    ;;
    *"v2ex"*)
      echo 'v2ex'
    ;;
    "null"|"")
      echo 'default'
    ;;
  esac
fi