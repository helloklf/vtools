#!/system/bin/sh

function module_installed() {
    if [[ -n "$MAGISK_PATH" ]] && [[ -d "$MAGISK_PATH" ]]
    then
        return 1
    else
        return 0
    fi
}

# 替换文件，用法如：
# magisk_replace_file "./kr-script/miui/resources/com.android.systemui" "/system/media/theme/default/com.android.systemui"
function magisk_replace_file() {
    local input="$1"
    local target="$2"
    local output="$MAGISK_PATH$target"
    if [[ -f "$input" ]]
    then
        local dir="$(dirname $output)"
        mkdir -p "$dir"
        cp "$input" "$output"
        chmod 755 "$output"
        magisk_file_equals "$input" "$target"
        local result="$?"
        return $result
    else
        echo "$input 不存在，无法复制到模块" 1>&2
        return 0
    fi
}

# 取消文件替换，用法如：
# magisk_cancel_replace "/system/media/theme/default/com.android.systemui"
function magisk_cancel_replace()
{
    local output="$MAGISK_PATH$1"
    if [[ -e "$output" ]]
    then
        rm -rf "$output"
    fi
    return 1
}

# 判断某个文件是否被模块替换
function magisk_file_exist()
{
    if [[ -f "$MAGISK_PATH$1" ]]
    then
        return 1
    else
        return 0
    fi
}

# 判断文件是否与模块中某个文件相同，用法
# magisk_file_equals "./kr-script/miui/resources/com.android.systemui" "/system/media/theme/default/com.android.systemui"
function magisk_file_equals()
{
    local input="$1"
    local output="$MAGISK_PATH$2"
    if [[ -f "$input" ]] && [[ -f "$output" ]]
    then
        local md5=`busybox md5sum $input | cut -f1 -d ' '`
        local verify=`busybox md5sum $output | cut -f1 -d ' '`
        if [[ "$md5" = "$verify" ]]
        then
            return 1
        else
            return 0
        fi
    else
        return 0
    fi
}

function magisk_set_system_prop() {
    if [[ -d "$MAGISK_PATH" ]];
    then
        $BUSYBOX sed -i "/$1=/"d "$MAGISK_PATH/system.prop"
        $BUSYBOX sed -i "\$a$1=$2" "$MAGISK_PATH/system.prop"
        setprop $1 $2 2> /dev/null
        return 1
    fi;
    return 0
}

function magisk_cancel_system_prop()
{
    if [[ -d "$MAGISK_PATH" ]];
    then
        $BUSYBOX sed -i "/$1=/"d "$MAGISK_PATH/system.prop"
        setprop $1 $2 2> /dev/null
        return 1
    fi;
    return 0
}