mode=`settings get secure anr_show_background`
if [[ "$mode" = "1" ]];
then
    echo 1;
else
    echo 0;
fi

