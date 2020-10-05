base_dir="/data/user/$ANDROID_UID/com.xiaomi.market/files"
res_dir=`ls $base_dir | grep web-res- | tail -1`

function override_file() {
  local page="$1"
  local css_override="$base_dir/$res_dir/$page.override.css"
  local html_file="$base_dir/$res_dir/$page.html"

  if [[ -f "$html_file" ]]; then
    echo "@import url(\"$page.chunk.css\");" > $css_override
    cat $PAGE_WORK_DIR/app_store/$page.css >> $css_override
    sed -i "s/$page.chunk.css/$page.override.css/" "$html_file"

    chmod 777 $css_override
    chmod 777 "$html_file"
  else
    echo '未找到' $html_file 1>&2
  fi

  echo ''
}

echo '在使用本功能前，请务必打开一次应用商店的各个界面~'
echo ''

if [[ "$res_dir" = "" ]];
then
  echo '请先启动一次应用商店，并浏览各个界面~' 1>&2
  exit 1
fi

echo '精简[应用详情]~'
override_file "detailV2"

echo '精简[必备]~'
override_file "essential"

echo '精简[游戏]~'
override_file "g-feature"

echo '精简[首页]~'
override_file "index"

echo '精简[我的]~'
override_file "mine"

echo '精简[排行榜]~'
override_file "rank"

echo '精简[搜索]~'
override_file "search-guide"

echo '精简[软件]~'
override_file "software"

killall -9 com.xiaomi.market 2>/dev/null


echo '如需还原，清空“应用商店”的数据即可~'