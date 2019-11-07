#!/system/bin/sh

# 参数说明
# $1 脚本路径
# $2 执行的目录

# 将要执行的具体脚本，执行 executor.sh 时传入，如 ./executor.sh test.sh
script_path="$1"
# 脚本执行目录（目前该参数已被废弃使用）
execute_path="$2"

# 定义全局变量
export EXECUTOR_PATH=$({EXECUTOR_PATH})
export START_DIR=$({START_DIR})
export TEMP_DIR=$({TEMP_DIR})
export ANDROID_UID=$({ANDROID_UID})
export ANDROID_SDK=$({ANDROID_SDK})
export SDCARD_PATH=$({SDCARD_PATH})
export BUSYBOX=$({BUSYBOX})
export MAGISK_PATH=$({MAGISK_PATH})
export PACKAGE_NAME=$({PACKAGE_NAME})
export PACKAGE_VERSION_NAME=$({PACKAGE_VERSION_NAME})
export PACKAGE_VERSION_CODE=$({PACKAGE_VERSION_CODE})

# toolkit工具目录
export TOOLKIT=$({TOOLKIT})
# 添加toolkit添加为应用程序目录
if [[ ! "$TOOLKIT" = "" ]]; then
    # export PATH="$PATH:$TOOLKIT"
    PATH="$PATH:$TOOLKIT"
fi

# 安装busybox完整功能
if [[ -f "$TOOLKIT/kr_install_busybox.sh" ]]; then
    sh "$TOOLKIT/kr_install_busybox.sh"
fi


# 判断是否有指定执行目录
if [[ "$execute_path" != "" ]] && [[ -d "$execute_path" ]]
then
    cd "$execute_path"
fi


# 运行脚本
if [[ -f "$script_path" ]]; then
    chmod 755 "$script_path"
    # sh "$script_path"     # 2019.09.02 before
    source "$script_path"   # 2019.09.02 after
else
    echo "${script_path} 已丢失" 1>&2
fi