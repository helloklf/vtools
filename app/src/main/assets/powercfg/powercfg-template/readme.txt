在这里，你可以为你的设备配置两套不同风格的模式文件。打包apk运行时，可以在性能配置界面中切换模式文件。

#保守风格的模式文件
powercfg-base-default.sh
powercfg-default.sh

#调度积极的模式文件
powercfg-base-bigcore.sh
powercfg-bigcore.sh


powercfg-base-bigcore.sh 和 powercfg-base-default.sh不会经常用到，只会在最开始的时候执行一次，用于还原一些用户已做的修改，如果没有必要可以留空。
powercfg-bigcore.sh 和 powercfg-default.sh作为主要配置脚本，需要在里面定义4种模式要执行的脚本代码。

如果你不想通过修改apk的方式来创建配置，也可以直接将写好的powercfg.sh，复制到data目录下【最终配置脚本路径为 /data/powercfg.sh】，并修改权限为0644。
注意配置脚本的编码格式，应为unix，否则无法被命令行识别，也就无法执行模式切换。