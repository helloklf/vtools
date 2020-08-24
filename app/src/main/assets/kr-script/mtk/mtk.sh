function xml_start() {
    echo '<?xml version="1.0" encoding="UTF-8" ?>'
    echo "<root>"
}
function xml_end() {
    echo "</root>"
}
function resource() {
    echo "  <resource dir=\"$1\" />"
}
function group_start() {
    echo "  <group title=\"$1\">"
}
function group_end() {
    echo "  </group>"
}
function switch() {
    echo "      <switch title=\"$1\">"
    echo "          <get>$2</get>"
    echo "          <set>$3</set>"
    echo "      </switch>"
}
function switch_hidden() {
    echo "      <switch title=\"$1\" shell=\"hidden\" >"
    echo "          <get>$2</get>"
    echo "          <set>$3</set>"
    echo "      </switch>"
}

function get_row_id() {
  local row_id=`echo $1 | cut -f1 -d ']'`
  echo ${row_id/[/}
}
function get_row_title() {
    echo $1 | cut -f2 -d ' ' | cut -f1 -d ':'
}
function get_row_state() {
    echo $1 | cut -f2 -d ':'
}

function ppm_render() {
    path="/proc/ppm/policy_status"
    cat $path | grep 'PPM_' | while read line
    do
      id=`get_row_id "$line"`
      title=`get_row_title "$line"`
      state=`get_row_state "$line"`
      # echo $id $title $state
      switch_hidden "$title" "cat $path | grep $title | grep enabled 1>&amp;2 > /dev/null &amp;&amp; echo 1" "echo $id \$state > $path"
    done
}

function ged_render() {
    local ged="/sys/module/ged/parameters"
    ls -1 $ged | grep -v "log" | grep -v "debug" | grep -E "enable|_on|mode" | while read line
    do
      # echo $line
      local title="$line"
      if [[ "$line" == "gpu_dvfs_enable" ]]; then
        title="动态调频调压"
      elif [[ "$line" == "ged_force_mdp_enable" ]]; then
        title="强制使用MDP"
      elif [[ "$line" == "gx_game_mode" ]]; then
        title="游戏模式"
      fi
      switch_hidden "$title" "cat $ged/$line" "echo \$state > $ged/$line"
    done
}

xml_start
    resource 'file:///android_asset/kr-script/common'
    resource 'file:///android_asset/kr-script/mtk'
    group_start 'PPM'
        ppm_render
    group_end

    group_start 'GED'
        ged_render
    group_end
xml_end
