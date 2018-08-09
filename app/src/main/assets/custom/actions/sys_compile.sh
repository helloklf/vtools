#!/system/bin/sh

for item in `pm list packages -s`
do
	app=${item:8}
	echo "compile -> $app"
	cmd package compile -m speed $app
done
