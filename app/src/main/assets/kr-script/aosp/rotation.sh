echo '关闭自动旋转'
# content insert --uri content://settings/system --bind name:s:accelerometer_rotation --bind value:i:0
settings put system accelerometer_rotation 0

echo '设置屏幕方向'
# content insert --uri content://settings/system --bind name:s:user_rotation --bind value:i:$state
settings put system user_rotation $state
