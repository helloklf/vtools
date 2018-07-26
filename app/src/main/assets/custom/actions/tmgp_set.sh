#!/system/bin/sh

#环境变量由Scene提供
#echo "TEMP_DIR=$TEMP_DIR"
#echo "ANDROID_UID=$ANDROID_UID"
#echo "ANDROID_SDK=$ANDROID_SDK"
#echo "SDCARD_PATH=$SDCARD_PATH"

USER_ID='0'
if [[ -n "${ANDROID_UID}" ]]; then
    USER_ID="${ANDROID_UID}";
fi;

dir="/data/user/${USER_ID}/com.tencent.tmgp.sgame/shared_prefs"
prefs="${dir}/com.tencent.tmgp.sgame.v2.playerprefs.xml"

if [ ! -f "$prefs" ]; then
  echo '没找到配置脚本，如果你刚更新了版本，请先启动一次游戏！！！'
  exit;
fi

echo '>>> 1.移除配置';
$BUSYBOX sed -i '/.*<int name="VulkanTryCount" value=".*" \/>/'d "$prefs"
$BUSYBOX sed -i '/.*<int name="EnableVulkan" value=".*" \/>/'d "$prefs"
$BUSYBOX sed -i '/.*<int name="EnableGLES3" value=".*" \/>/'d "$prefs"
$BUSYBOX sed -i '/.*<int name="EnableMTR" value=".*" \/>/'d "$prefs"
$BUSYBOX sed -i '/.*<int name="DisableMTR" value=".*" \/>/'d "$prefs"

echo '>>> 2.更新配置...';

if [[ "$render" = 'VT' ]]; then
    $BUSYBOX sed  -i '2a \ \ \ \ <int name="VulkanTryCount" value="1" \/>' "$prefs";
    $BUSYBOX sed  -i '3a \ \ \ \ <int name="EnableVulkan" value="1" \/>' "$prefs";
    $BUSYBOX sed  -i '4a \ \ \ \ <int name="EnableGLES3" value="3" \/>' "$prefs";
elif [[ "$render" = 'VTF' ]]; then
    $BUSYBOX sed  -i '2a \ \ \ \ <int name="VulkanTryCount" value="1" \/>' "$prefs";
    $BUSYBOX sed  -i '3a \ \ \ \ <int name="EnableVulkan" value="2" \/>' "$prefs";
    $BUSYBOX sed  -i '4a \ \ \ \ <int name="EnableGLES3" value="3" \/>' "$prefs";
elif [[ "$render" = 'O3T' ]]; then
    $BUSYBOX sed  -i '2a \ \ \ \ <int name="VulkanTryCount" value="1" \/>' "$prefs";
    $BUSYBOX sed  -i '3a \ \ \ \ <int name="EnableVulkan" value="3" \/>' "$prefs";
    $BUSYBOX sed  -i '4a \ \ \ \ <int name="EnableGLES3" value="1" \/>' "$prefs";
elif [[ "$render" = 'O3TF' ]]; then
    $BUSYBOX sed  -i '2a \ \ \ \ <int name="VulkanTryCount" value="1" \/>' "$prefs";
    $BUSYBOX sed  -i '3a \ \ \ \ <int name="EnableVulkan" value="3" \/>' "$prefs";
    $BUSYBOX sed  -i '4a \ \ \ \ <int name="EnableGLES3" value="2" \/>' "$prefs";
elif [ "$render" = 'O2T' ]; then
    $BUSYBOX sed  -i '2a \ \ \ \ <int name="VulkanTryCount" value="3" \/>' "$prefs";
    $BUSYBOX sed  -i '3a \ \ \ \ <int name="EnableVulkan" value="3" \/>' "$prefs";
    $BUSYBOX sed  -i '4a \ \ \ \ <int name="EnableGLES3" value="3" \/>' "$prefs";
fi

if [[ "$thread" = "1" ]]; then
    $BUSYBOX sed  -i '5a \ \ \ \ <int name="EnableMTR" value="1" \/>' "$prefs";
    $BUSYBOX sed  -i '6a \ \ \ \ <int name="DisableMTR" value="3" \/>' "$prefs";
else
    $BUSYBOX sed  -i '5a \ \ \ \ <int name="EnableMTR" value="3" \/>' "$prefs";
    $BUSYBOX sed  -i '6a \ \ \ \ <int name="DisableMTR" value="1" \/>' "$prefs";
fi;

echo '>>> 3.修正权限...'
cd $dir
chown -R -L `ls -ld|cut -f3 -d ' '`:`toybox ls -ld|cut -f4 -d ' '` "$prefs";
chown -R -L `toybox ls -ld|cut -f3 -d ' '`:`toybox ls -ld|cut -f4 -d ' '` "$prefs";
chmod 660 $prefs;

echo '>>> 4.启动游戏...'
echo ' '
echo ' '

echo '* 注意：本修改可能只会在下次启动游戏时生效一次，因为王者荣耀会根据你的机型自动重置设置，如需永久生效，则需要伪装成特定的机型，具体操作可在酷安市场上搜索！'
echo ' '
echo ' '

am kill --user "${USER_ID}" com.tencent.tmgp.sgame
sync
am start -n com.tencent.tmgp.sgame/.SGameActivity


#下方两种方式可能会把所有用户的王者荣耀全杀掉
#killall -9 com.tencent.tmgp.sgame
#pkill -9 com.tencent.tmgp.sgame


