#!/system/bin/sh

platform=`getprop ro.board.platform`

if [[ "$platform" = "sdm845" ]]
then
    echo 'sdm845|骁龙845'
elif [[ "$platform" = "sdm710" ]]
then
    echo 'sdm710|骁龙710AIE'
elif [[ "$platform" = "kona" ]]
then
    echo 'kona|骁龙865'
elif [[ "$platform" = "msmnile" ]]
then
    echo 'msmnile|骁龙855'
elif [[ "$platform" = "sm6150" ]]
then
    echo 'sm6150|骁龙730'
elif [[ "$platform" = "msm8998" ]]
then
    echo 'msm8998|骁龙835'
elif [[ "$platform" = "msm8996" ]]
then
    echo 'msm8996|骁龙820\821'
elif [[ "$platform" = "mt6873" ]]
then
    echo 'mt6873|天玑800\820'
fi
