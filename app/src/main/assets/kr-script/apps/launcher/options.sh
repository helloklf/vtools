#!/system/bin/sh

# pm query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME

# launchers=$(pm query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME | grep '/' | sed 's/\ //g')

# 获取安装的所有桌面应用
launchers=$(pm query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME | grep '/')

for launcher in $launchers ; do
    packageName=`echo $launcher | cut -f1 -d '/'`
    echo "$launcher|$packageName"
done
