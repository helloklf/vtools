if [[ "$1" == "" ]] || [[ "$2" == "" ]];then
  return
fi

mode="$1"
delay="$3"

if [[ ! -f "$2" ]]; then
  return
fi

# freeze_apps=""
source $2


if [[ "$delay" != "" ]]; then
  uuid=`date "+%Y%m%d%H%M%S"`
  setprop vtools.freeze_delay "$uuid"

  sleep $delay

  last_id=`getprop vtools.freeze_delay`
  if [[ "$last_id" != "$uuid" ]]; then
    return
  fi
fi


for app in $freeze_apps; do
  if [[ "$app" == "com.android.vending" ]]; then
    pm disable com.google.android.gsf 2> /dev/null
    pm disable com.google.android.gsf.login 2> /dev/null
    pm disable com.google.android.gms 2> /dev/null
    pm disable com.android.vending 2> /dev/null
    pm disable com.google.android.play.games 2> /dev/null
    pm disable com.google.android.syncadapters.contacts 2> /dev/null
  elif [[ "$mode" == "suspend" ]]; then
    pm suspend ${app} 2> /dev/null
    am force-stop ${app} 2> /dev/null
    am kill current ${app} 2> /dev/null
  else
    pm disable ${app} 2> /dev/null
  fi
done
