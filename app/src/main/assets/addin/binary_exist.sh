if [[ $(type $1 2> /dev/null | grep -v 'not found') != "" ]]; then
  echo true
else
  echo false
fi
