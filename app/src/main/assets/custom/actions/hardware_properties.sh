#!/system/bin/sh

# dumpsys hardware_properties
# ****** Dump of HardwarePropertiesManagerService ******
# CPU temperatures: [34.5, 34.800003, 34.2, 34.2, 34.800003, 34.5, 33.5, 34.2] # cpu温度
# CPU throttling temperatures: [60.0, 60.0, 60.0, 60.0, 60.0, 60.0, 60.0, 60.0] # cpu 限制温度
# CPU shutdown temperatures: [115.0, 115.0, 115.0, 115.0, 115.0, 115.0, 115.0, 115.0] # 关机cpu温度
# CPU vr throttling temperatures: [-3.4028235E38, -3.4028235E38, -3.4028235E38, -3.4028235E38, -3.4028235E38, -3.4028235E38, -3.4028235E38, -3.4028235E38] # ？？？
# GPU temperatures: [34.2] # GPU温度
# GPU throttling temperatures: [-3.4028235E38]
# GPU shutdown temperatures: [-3.4028235E38]
# GPU vr throttling temperatures: [-3.4028235E38]
# Battery temperatures: [3.9] # 电池温度
# Battery throttling temperatures: [-3.4028235E38]
# Battery shutdown temperatures: [60.0] # 关机电池温度
# Battery vr throttling temperatures: [-3.4028235E38]
# Skin temperatures: [34.0] # 表面温度
# Skin throttling temperatures: [44.0] # 表面温度限制
# Skin shutdown temperatures: [70.0] # 关机时的表面温度
# Skin vr throttling temperatures: [58.0]
# Fan speed: [] # 风扇转速
#
# Cpu usage of core: 0, active = 268088, total = 1159435
# Cpu usage of core: 1, active = 242269, total = 283318
# Cpu usage of core: 2, active = 176735, total = 224656
# Cpu usage of core: 3, active = 171445, total = 220481
# Cpu usage of core: 4, active = 53628, total = 119422
# Cpu usage of core: 5, active = 46756, total = 112736
# Cpu usage of core: 6, active = 60577, total = 122101
# Cpu usage of core: 7, active = 59014, total = 121383
# ****** End of HardwarePropertiesManagerService dump ******

cache=/cache/hardware_properties

dumpsys hardware_properties > $cache

while read line
do
   property=`echo $line | cut -f1 -d ':'`
   value=`echo $line | cut -f2 -d ':'`

   case "$property" in
   "CPU temperatures")
        echo "CPU核心温度：" $value
   ;;
   "CPU throttling temperatures")
        echo "CPU限制温度：" $value
   ;;
   "CPU shutdown temperatures")
        echo "CPU关机温度：" $value
   ;;
   "GPU temperatures")
        echo "CPU核心温度：" $value
   ;;
   "Battery temperatures")
        echo "电池温度：" $value
   ;;
   "Skin temperatures")
        echo "表面温度：" $value
   ;;
   esac
done  < $cache
