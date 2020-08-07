#! /vendor/bin/sh

MODDIR=${0%/*}

setprop sys.lmk.minfree_levels 12800:0,16384:100,18432:200,24576:250,32768:900,49152:950

stop lmkd
# 
start lmkd
