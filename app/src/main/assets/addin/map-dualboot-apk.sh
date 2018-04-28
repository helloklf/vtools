#!/bin/sh
##!/system/bin/sh
sleep 3

if test -d /raw/data
then
root_dir="/raw"
else
root_dir=""
fi

multiboot_dir="${root_dir}/data/multiboot"11
apk_so_lib=()
apk_oat_odex=()

function echo_fileinfo()
{
   echo "[compute-md5 $1]"
   inode=`ls -li $1 |cut -f1 -d " "`
   md5=`md5sum $1|cut -f1 -d " "`
   size=`busybox du $1|busybox awk '{print $1}'`
   echo "[fileinfo:$inode;$md5;$1;$size]"
}

echo "[mapfiles]"

function add_app_odex()
{
    if test -d $1/oat
    then
        for lib_path in $1/oat/*.odex
        do
            if test -f ${lib_path}
            then
                echo_fileinfo $lib_path
            fi
        done

        if test -d $1/oat/arm
        then
            for lib_path in $1/oat/arm/*.odex
            do
                if test -f ${lib_path}
                then
                    echo_fileinfo $lib_path
                fi
            done
        fi

        if test -d $1/oat/arm64
        then
            for lib_path in $1/oat/arm64/*.odex
            do
                if test -f ${lib_path}
                then
                    echo_fileinfo $lib_path
                fi
            done
        fi
    fi
}

function add_app_solib()
{
    echo [mapfiles $1/lib]
    if test -d $1/lib
    then
        echo [mapfiles $1/lib]
        for lib_path in $1/lib/*.so
        do
            if test -f ${lib_path}
            then
                echo_fileinfo $lib_path
            fi
        done
        if test -d $1/lib/arm
        then
            echo [mapfiles $1/lib/arm]
            for lib_path in $1/lib/arm/*.so
            do
                if test -f ${lib_path}
                then
                    echo_fileinfo $lib_path
                fi
            done
        fi
        if test -d $1/lib/arm64
        then
            echo [mapfiles $1/lib/arm64]
            for lib_path in $1/lib/arm64/*.so
            do
                if test -f ${lib_path}
                then
                    echo_fileinfo $lib_path
                fi
            done
        fi
    fi
}

function get_one_system_apps ()
{
    if test -d $1/data/app
    then
        for apk_path in $1/data/app/*
        do
            if test -d "${apk_path}"
            then
                for apk_path_two in ${apk_path}/*.apk
                do
                    if test -f ${apk_path_two}
                    then
                        echo_fileinfo ${apk_path_two}
                    fi
                done
                add_app_solib ${apk_path}
                #add_app_odex ${apk_path}
            fi
        done
        for apk_path in $1/data/app/*.apk
        do
            if test -f "${apk_path}"
            then
                echo_fileinfo ${apk_path}
            fi
        done
    fi
}

#esFileManager or vTools backup dir
#for apk_path in ${root_dir}/data/media/0/backups/apps/*.apk
#do
#    if test -f "${apk_path}"
#    then
#        echo_fileinfo ${apk_path}
#    fi
#done

#Search Apk or solib on DualBoot System
function get_multiboot_apps ()
{
    for dbp_sys_path in $multiboot_dir/*
    do
        if test -d $dbp_sys_path
        then
            get_one_system_apps ${dbp_sys_path}
        fi
    done
}

#Search Apk or solib on Primary System
get_one_system_apps $root_dir


#
if test -d $multiboot_dir
then
    get_multiboot_apps
fi

#echo "[operation completed]";
