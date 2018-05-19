#!/system/bin/sh

action=$1
dir='/data/data/com.tencent.tmgp.sgame/shared_prefs'
prefs='/data/data/com.tencent.tmgp.sgame/shared_prefs/com.tencent.tmgp.sgame.v2.playerprefs.xml'
if [ ! -f $prefs ]; then
  echo '没找到配置文件，如果你刚更新了版本，请先启动一次游戏！！！'
  exit;
fi

busybox sed -i '/.*<int name="VulkanTryCount" value=".*" \/>/'d $prefs
busybox sed -i '/.*<int name="EnableVulkan" value=".*" \/>/'d $prefs
busybox sed -i '/.*<int name="EnableGLES3" value=".*" \/>/'d $prefs
busybox sed -i '/.*<int name="EnableMTR" value=".*" \/>/'d $prefs
busybox sed -i '/.*<int name="DisableMTR" value=".*" \/>/'d $prefs

echo '>>> 1.修改配置为 ' $action;

if [ $action = 'VT' ]; then
    busybox sed  -i '2a \ \ \ \ <int name="VulkanTryCount" value="1" \/>' $prefs;
    busybox sed  -i '3a \ \ \ \ <int name="EnableVulkan" value="1" \/>' $prefs;
    busybox sed  -i '4a \ \ \ \ <int name="EnableGLES3" value="3" \/>' $prefs;
elif [ $action = 'VTF' ]; then
    busybox sed  -i '2a \ \ \ \ <int name="VulkanTryCount" value="1" \/>' $prefs;
    busybox sed  -i '3a \ \ \ \ <int name="EnableVulkan" value="2" \/>' $prefs;
    busybox sed  -i '4a \ \ \ \ <int name="EnableGLES3" value="3" \/>' $prefs;
elif [ $action = 'O3T' ]; then
    busybox sed  -i '2a \ \ \ \ <int name="VulkanTryCount" value="1" \/>' $prefs;
    busybox sed  -i '3a \ \ \ \ <int name="EnableVulkan" value="3" \/>' $prefs;
    busybox sed  -i '4a \ \ \ \ <int name="EnableGLES3" value="1" \/>' $prefs;
elif [ $action = 'O3TF' ]; then
    busybox sed  -i '2a \ \ \ \ <int name="VulkanTryCount" value="1" \/>' $prefs;
    busybox sed  -i '3a \ \ \ \ <int name="EnableVulkan" value="3" \/>' $prefs;
    busybox sed  -i '4a \ \ \ \ <int name="EnableGLES3" value="2" \/>' $prefs;
elif [ $action = 'O2T' ]; then
    busybox sed  -i '2a \ \ \ \ <int name="VulkanTryCount" value="3" \/>' $prefs;
    busybox sed  -i '3a \ \ \ \ <int name="EnableVulkan" value="3" \/>' $prefs;
    busybox sed  -i '4a \ \ \ \ <int name="EnableGLES3" value="3" \/>' $prefs;
fi

busybox sed  -i '5a \ \ \ \ <int name="EnableMTR" value="1" \/>' $prefs;
busybox sed  -i '6a \ \ \ \ <int name="DisableMTR" value="3" \/>' $prefs;

echo '>>> 2.修正权限'
cd $dir
chown -R -L `toybox ls -ld|cut -f3 -d ' '`:`toybox ls -ld|cut -f4 -d ' '` $prefs;
chmod 660 $prefs;


echo '>>> 3.现在，请手动重启游戏吧！'
echo ' '
echo ' '

echo '* 注意：本修改可能只会在下次启动游戏时生效一次，因为王者荣耀会根据你的机型自动重置设置，如需永久生效，则需要伪装成特定的机型，具体操作可在酷安市场上搜索！'

echo ' '
echo ' '


#killall -9 com.tencent.tmgp.sgame
#pkill -9 com.tencent.tmgp.sgame

