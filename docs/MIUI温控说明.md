## MIUI温控修改器说明
- 这里主要说修改MIUI温控的一些技巧
- 至于温控配置文件的语法格式，可在百度上搜索学习，所有高通机型都通用
- 此外，已知Redmi 10x虽然是天玑处理器，但也可以使用相似的语法配置温控

### 配置存储
- 通常，MIUI系统下会有类似如下列表的多个温控配置文件
- 因SOC和适配的场景数不同，可能会有增有减，文件名称也稍有差异，但均以类似于`thermal-*tgame.conf`这样的名称出现
  ```
  thermal-engine-sdm845-normal.conf
  thermal-engine-sdm845-nolimits.conf
  thermal-engine-sdm845-tgame.conf
  thermal-engine-sdm845-sgame.conf
  thermal-engine-sdm845-extreme.conf
  thermal-engine-sdm845.conf
  ```
- 温控配置文件一般存储在 `/vendor/etc` 下，旧版(Android8.0以前)系统可能在`/system/etc`
- 此外，温控配置文件还可能出现在
  > `/data/vendor/thermal/config`，通常由云端下发，如果有的话，会优先使用<br />
  > 我们修改温控的时候，可以直接替换到`/vendor/etc`，也可以放到`/data/vendor/thermal/config`
  >

### 文件释义
- 从文件名就不难看出，这每一个配置文件都对应到不同的使用场景
- 例如`*-normal.conf`表示默认温控，也就是绝大多数普通应用所使用的温控配置
  > 部分机型是没有`*-normal.conf`这个文件，例如：<br />
  > sdm845 默认温控是`thermal-engine-sdm845.conf`<br />
  > sdm710 默认温控是`thermal-engine-sdm710.conf`

- 常见的几个温控配置名称释义
  ```
  -normal.conf    # 默认温控
  -nolimits.conf  # 无限制(跑分)
  -tgame.conf     # 游戏
  -sgame.conf     # 王者荣耀
  -extreme.conf   # 极致省电
  -pubgmhd.conf   # 刺激战场
  ```

### 说明示例
#### - 举例 ①
- 下面这个是米10Pro默认温控的第一个片段
  ```conf
  [VIRTUAL-SENSOR]
  algo_type	virtual
  sensors  cam_therm1  battery  conn_therm  quiet_therm  wireless_therm  xo_therm
  weight  1149  147  -193  408    -228      -385
  polling  1000
  weight_sum	1000
  compensation	2222
  ```
- algo_type `virtual` 表示虚拟一个传感器，`[VIRTUAL-SENSOR]`是这组配置的名称
- 而sensors定义了`cam_therm1`，`battery`，`conn_therm`，`quiet_therm`，`wireless_therm`，`xo_therm` 多达6个传感器， weight 表示各个传感器的数值权重，polling 表示每1000ms轮询一次
- 简单的说，就是通过读取多个传感器的数值，分别使用不同权重，最终计算得到一个虚拟的温度数值

#### 举例 ②
- 下面这个是米10Pro的的一段CPU温控配置示例
  ```conf
  [SS-CPU4]
  algo_type	ss
  sensor  VIRTUAL-SENSOR
  device  cpu4
  polling  1000
  trig  37000  38000  39000  41000  43000  49000
  clr  35000  37000  38000  39000  41000  47000
  target  1862400  1766400  1574400  1478400  1286400  710400
  ```
- 可以看到这里的 sensor 写的是 `VIRTUAL-SENSOR`，也就是上一个片段里定义的虚拟传感器
- 当然，温控定义所使用的传感器是可以更换的（但不建议），例如：
  ```conf
  [SS-CPU4]
  algo_type	ss
  sensor  battery
  device  cpu4
  polling  1000
  trig  37000  38000  39000  41000  43000  49000
  clr  35000  37000  38000  39000  41000  47000
  target  1862400  1766400  1574400  1478400  1286400  710400
  ```

- `需要注意的是，不同传感器可能出现不同的温度单位，有可能出现37000表示37°C，也有可能是37，甚至可能是370。如果想更换传感器，千万留心！`

#### 举例 ③
- 下面这个是米10Pro的的一段低电量关核降频配置示例
  ```conf
  [MONITOR-BCL]
  algo_type	monitor
  sensor  BAT_SOC
  device  cpu4+hotplug_cpu6+hotplug_cpu7
  polling  2000
  trig  5
  clr  6
  target  1286400+1+1
  reverse  1
  ```
- 这里的sensor是`BAT_SOC`，依然是一个虚拟传感器，其定义是
  ```conf
  [BAT_SOC]
  algo_type	simulated
  path  /sys/class/power_supply/battery/capacity
  polling  10000
  ```
- trig `5` 表示 `电量 <= 5%` 时触发限制，clr  `6` 表示 `电量 >= 5%` 时清除限制
- 这段配置表示的是，电量<=5%时，cpu4频率降低到1.2Ghz，并关闭cpu6、cpu7

#### 示例 ④
- 下面这个是米10Pro的的一温度配置示例
  ```conf
  [MONITOR-TEMP_STATE]
  algo_type	monitor
  sensor  VIRTUAL-SENSOR
  device  temp_state
  polling  2000
  trig  45000  53000
  clr  44000  51000
  target  10100000	12400001
  ```
- 温度状态表示的是温度达到指定值后触发的一系列限制
- 例如，温度过高可能会同时禁止使用相机、闪光灯、HBM等
- 不过由于`target`所指向的数值并不在这里定义，具体会触发何种限制就不得而知了

### 其它提示
- 目前，部分机型的新版MIUI已经支持温控即时替换
- 只需将修改后的温控配置复制到`/data/vendor/thermal/config`
- 系统就会自动应用当前命中的场景所对应的配置
- 最简单的，你可以留意`/data/vendor/thermal/decrypt.txt`是否产生变化来确定温控是否已生效


### 测试和日志
- 大多数情况下，MIUI会在`/data/vendor/thermal/`下记录温控切换日志和温度限制触发和清除日志，可以直接查看了解温控的运行过程
- 也可以通过 `thermal-engine -o > /cache/thermal.conf` 获得即时状态
- 当然，thermal-engine还有其它一些命令，如：
```sh
# thermal-engine --help
Temperature sensor daemon
Optional arguments:
  --config-file/-c <file>        config file
  --debug/-d                     debug output
  --soc_id/-s <target>           target soc_id
  --norestorebrightness/-r       disable restore brightness functionality
  --output-conf/-o               dump config file of active settings
  --trace/-t                     enable ftrace tracing
  --dump-bcl/-i                  BCL ibat/imax file
  --help/-h                      this help screen
```


### 相关资料
- https://blog.csdn.net/LoongEmbedded/article/details/55049975?utm_source=blogxgwz8
- https://blog.csdn.net/bs66702207/article/details/72782431?utm_source=blogxgwz0
- https://www.geek-share.com/detail/2700066916.html
- http://www.mamicode.com/info-detail-2022213.html
