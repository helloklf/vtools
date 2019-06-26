#!/system/bin/sh

for item in `pm list packages -s`
do
	app=${item:8}
	echo "> $app"
	cmd package compile -m speed $app 1>/dev/null
done
