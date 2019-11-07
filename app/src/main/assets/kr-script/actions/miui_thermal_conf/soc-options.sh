#!/system/bin/sh

platform=`getprop ro.board.platform`

if [[ "$platform" = "sdm845" ]]
then
    echo 'sdm845|骁龙845'
elif [[ "$platform" = "sdm710" ]]
then
    echo 'sdm710|骁龙710AIE'
elif [[ "$platform" = "msmnile" ]]
then
    echo 'msmnile|骁龙855'
elif [[ "$platform" = "msm8998" ]]
then
    echo '8998|骁龙835'
fi
