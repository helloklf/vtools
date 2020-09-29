css=".c_functionlist-high {
    box-shadow: none;
    display: flex;
    flex-wrap: wrap;
    padding-top: 0;
}
.c_functionlist-high::before {
    content: '';
    display: none !important;
}

.c_functionlist_item {
    width: 50%;
}
.c_functionlist_item:focus {
    outline: none;
}

.c_functionlist_item .functionlist_icon {
    margin-top: 2vw;
    width: 15vw;
    height: 15vw;
}

.c_functionlist_item .functionlist_title {
    font-weight: 600;
    font-size: 3.5vw;
}

.c_functionlist_item .functionlist_desc {
    font-size: 2.5vw;
    margin-bottom: 1vw;
    margin-top: 0.5vw;
    opacity: 0.5;
}

.load.load-bottom {
    display: none !important;
}

.c_update {
    padding-top: 5px;
    padding-bottom: 5px;
    background-color: #f8f8f8;
}
.c_functionlist-high .c_functionlist_update {
    width: 100%;
    margin: 2vh 0;
    order: -1;
}

.c_update_title {
    font-size: 4vw;
    font-weight: 600;
}

.c_functionlist_item:nth-child(1),
.c_functionlist_item:nth-child(4),
.c_functionlist_item:nth-child(5),
.c_functionlist_item:nth-child(8) {
    background-color: rgba(128, 128, 128, 0.05);
}

.c_functionlist_item:nth-child(2),
.c_functionlist_item:nth-child(3),
.c_functionlist_item:nth-child(6),
.c_functionlist_item:nth-child(7) {
    background-color: rgba(128, 128, 128, 0.01);
}

.c_page, .c_lazyloaded {
    display: none !important;
}
"

base_dir="/data/data/com.xiaomi.market/files"
res_dir=`ls $base_dir | grep web-res- | tail -1`
function override()
{
  local css_override="$base_dir/$res_dir/$2.override.css"
  local html_file="$base_dir/$res_dir/$2.html"

  echo "@import url(\"$2.chunk.css\");" > $css_override
  echo "$1" >> $css_override
  sed -i "s/$2.chunk.css/$2.override.css/" "$html_file"

  chmod 777 $css_override
  chmod 777 "$html_file"
}

override "$css" "mine"

killall -9 com.xiaomi.market 2>/dev/null
