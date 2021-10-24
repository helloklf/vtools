mode=$(settings get global captive_portal_mode)
if [[ "$mode" == "0" ]]; then
  echo '已禁用网络连通性检测'
else
  echo -n '使用:'
  url=$(settings get global captive_portal_https_url)
  case "$url" in
    *"google"*)
      echo 'Google服务器'
    ;;
    *"miui"*)
      echo 'MIUI服务器'
    ;;
    *"v2ex"*)
      echo 'V2EX服务器'
    ;;
    "null"|"")
      echo '默认服务器'
    ;;
  esac
fi