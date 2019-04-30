#!/system/bin/sh

# 参数说明
# $1 脚本路径
# $2 执行的目录

# 在environment里定义环境变量
export TEMP_DIR=${TEMP_DIR}
export ANDROID_UID=${ANDROID_UID}
export ANDROID_SDK=${ANDROID_SDK}
export SDCARD_PATH=${SDCARD_PATH}
export BUSYBOX=${BUSYBOX}
export MAGISK_PATH=${MAGISK_PATH}

# 将要执行的具体脚本，执行environment.sh时传入，如 ./krshell_environment.sh test.sh
script_path="$1"

# 脚本执行目录
execute_path="$2"

if [[ "$execute_path" != "" ]] && [[ -d "$execute_path" ]]
then
    cd "$execute_path"
fi

chmod 755 "$script_path"

sh "$script_path"
