#!/system/bin/sh

echo '编译模式：' $mode
echo '开始执行...'
echo ''

for item in `pm list packages $app`
do
	app=${item:8}
	echo "> $app"
	cmd package compile -m $mode $app 1>/dev/null
done
