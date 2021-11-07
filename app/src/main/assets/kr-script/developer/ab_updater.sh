# example
# rom=/sdcard/Download/miui_STAR_V12.5.20.0.RKACNXM_942a5712ef_11.0.zip

if [[ ! -f /cache/7za ]]; then
  echo '请放置 7za 二进制文件到/cache目录下！' 1>&2
  exit
fi

alias 7za="/cache/7za"

if [[ "$rom" == "" ]] || [[ ! -f "$rom" ]]; then
  echo '未选择ROM文件，或指定的文件无法访问！' 1>&2
  echo "选择的文件：$rom" 1>&2
  exit 1
fi

out_dir=${rom%.*}
if [[ "$out_dir" == "" ]]; then
  echo "路径解析失败 $out_dir" 1>&2
  exit 1
elif [[ -e "$out_dir" ]]; then
  echo "解压路径已存在 $out_dir" 1>&2
  exit 1
fi

files=$(7za l $rom)
if [[ $(echo "$files" | grep payload.bin) == "" ]] && [[ $(echo "$files" | grep payload_properties.txt) == "" ]]; then
  echo '压缩文件无效：payload.bin或payload_properties.txt缺失' 1>&2
  exit 1
fi

echo '解压ROM……'
7za e -o"$out_dir" "$rom" > /dev/null

if [[ ! -f "$out_dir/payload.bin" ]] && [[ ! -f "$out_dir/payload_properties.txt" ]]; then
  echo '解压失败：payload.bin或payload_properties.txt缺失' 1>&2
  exit 1
fi

echo '\n\n'
echo '即将开始更新系统，根据设备性能，可能需要5~10分钟甚至更久'
echo '你可以触摸日志输出区域，使屏幕保持点亮，但不要随意点击其它按钮。'
echo '在此期间(输出onPayloadApplicationComplete(ErrorCode::……之前)请勿操作手机'
echo '出现[INFO:……UPDATE_STATUS_DOWNLOADING (x), x.xxxxxx……]红色文字时，不要惊慌，这只是正常的进度显示！' 1>&2
echo '更新完成后，不建议在重启前安装Magisk，因为时常会导致数据异常需要恢复出厂设置'

# slot=$(getprop ro.boot.slot_suffix)
# echo -n '当前插槽：' $slot
# if [[ "$slot" == "_a" ]]; then
#   echo '，新系统将会安装到：_b'
# else
#   echo '，新系统将会安装到：_a'
# fi
# echo '\n'

sleep 15
# echo 'progress:[-1/100]'

headers=$(cat "$out_dir/payload_properties.txt")
update_engine_client --follow --update --payload="file://$out_dir/payload.bin" --headers="$headers" # --verify=false

echo '请自行参照错误码(ErrorCode)，判断更新是否成功'
echo 'kSuccess(0) 表示更新成功 ^_^'
echo '其它错误码均表示更新失败！'
