loc=`pm get-install-location | cut -f1 -d '['`
if [[ "$loc" = "0" ]]; then
    echo '自动（系统自动决定）'
elif [[ "$loc" = "1" ]]; then
    echo '内部存储（本机）'
elif [[ "$loc" = "2" ]]; then
    echo '外部存储(SD卡)'
fi