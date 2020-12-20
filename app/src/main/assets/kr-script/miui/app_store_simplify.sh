base_dir="/data/user/$ANDROID_UID/com.xiaomi.market/files"
res_dir=`ls $base_dir | grep web-res- | tail -1`
if [[ "$res_dir" == "" ]]; then
    echo '找到资源文件夹' 1>&2
    return
fi
echo 找到资源文件夹 $res_dir

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

echo '本功能最佳适配：'
echo '应用商店20.9.14(4001240)，web-res-1749'
echo ''

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

versionCode=`dumpsys package com.xiaomi.market | grep versionCode | cut -f2 -d '=' | cut -f1 -d ' ' | head -1`
if [[ $versionCode < 4001102 ]] || [[ $versionCode > 4001240 ]]
then
  echo '你的[应用商店]不是本功能最佳适配版本，部分修改可能不生效~'
  # 最佳适配 4001240，web-res-1749
fi
