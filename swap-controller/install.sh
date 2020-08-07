SKIPMOUNT=false
PROPFILE=true
POSTFSDATA=true
LATESTARTSERVICE=true

# Construct your own list here
REPLACE="
"

print_modname() {
  ui_print "*******************************"
  ui_print "     Module author: 嘟嘟ski    "
  ui_print "*******************************"
  ui_print " "
  ui_print " 配置参数位于 /data/swap_config.conf "
  ui_print " 可自行修改 "
  ui_print " "
  ui_print " "
}

# Copy/extract your module files into $MODPATH in on_install.

set_permissions() {
  # The following is the default rule, DO NOT remove
  set_perm_recursive $MODPATH/system 0 0 0755 0644

  # Here are some examples:
  # set_perm_recursive  $MODPATH/system/lib       0     0       0755      0644
  # set_perm  $MODPATH/system/bin/app_process32   0     2000    0755      u:object_r:zygote_exec:s0
  # set_perm  $MODPATH/system/bin/dex2oat         0     2000    0755      u:object_r:dex2oat_exec:s0
  # set_perm  $MODPATH/system/lib/libart.so       0     0       0644
}

on_install() {
  # The following is the default implementation: extract $ZIPFILE/system to $MODPATH
  # Extend/change the logic to whatever you want
  ui_print "- Extracting module files"
  unzip -o "$ZIPFILE" 'system/*' -d $MODPATH >&2
}

echo 1 > /data/swap_recreate

echo "
# 是否启用swap
swap=true
# swap大小(MB)
swap_size=2048
# swap使用顺序（0:与zram同时使用，-1:用完zram后再使用，5:优先于zram使用）
swap_priority=0
# 是否挂载为环回设备(如非必要，不建议开启)
swap_use_loop=false

# 是否启用zram
zram=false
# zram 大小(MB)
zram_size=2048
# zram压缩算法(可设置的值取决于内核支持)
comp_algorithm=lzo

# 使用zram、swap的积极性
swappiness=100
# 额外空余内存(kbytes)
extra_free_kbytes=98304
" > /data/swap_config.conf
