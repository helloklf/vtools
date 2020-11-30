sdcard='/data/media/0/'
target='Android/data/com.omarea.vtools/backups/apps/'
origin='backups/apps/'

cd $sdcard
mkdir -p $target
ls $origin | while read file; do
  if [[ -f "$origin$file" ]]; then
    echo $file
    ln -f "$origin$file" "$target$file"
  fi
done
