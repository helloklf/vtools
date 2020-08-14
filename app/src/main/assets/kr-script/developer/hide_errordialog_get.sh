mode=`settings get global hide_error_dialogs`
if [[ "$mode" = "1" ]];
then
    echo 1;
else
    echo 0;
fi

