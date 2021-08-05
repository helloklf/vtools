import com.sun.javaws.exceptions.ExitException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class Main {
    private static String readAllText(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bytes = new byte[20480];
            int count = fileInputStream.read(bytes);
            fileInputStream.close();
            return new String(bytes, 0, count, Charset.defaultCharset()).trim();
        } catch (IOException ex) {
            System.out.println("ReadAllText Fail:" + ex.getMessage());
            return "";
        }
    }

    private static int getOomADJ(String pid) {
        String adj = readAllText(new File("/proc/" + pid + "/oom_adj"));
        if (adj.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(adj);
        } catch (Exception ex) {
            System.out.println("GetOomADJ" + ex.getMessage());
        }
        return -1;
    }

    private static boolean include(String[] arr, String value) {
        for (String s : arr) {
            if (s.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static void writePid(String pid, File groupProcs) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(groupProcs);
            byte[] bytes = pid.getBytes();
            fileOutputStream.write(bytes, 0, bytes.length);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException ex) {
            System.out.println("WritePid Fail: " + ex.getMessage());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        String cgroupReclaim = args.length > 0 ? args[0].trim() : "active";
        String bgCGroup = "scene_bg";
        if (cgroupReclaim.equals("passive")) {
            bgCGroup = "scene_lock";
        } else if (cgroupReclaim.equals("force")) {
            bgCGroup = "scene_cache";
        }

        String memcg = "";
        if (new File("/sys/fs/cgroup/memory").exists()) {
            memcg = "/sys/fs/cgroup/memory";
        } else if (new File("/dev/memcg").exists()) {
            memcg = "/dev/memcg";
        }

        File fTopProcs = new File("/dev/cpuset/top-app/cgroup.procs");
        File fBgProcs = new File("/dev/cpuset/background/cgroup.procs");
        File fgGroup = new File(memcg + "/scene_fg/cgroup.procs");
        File bgGroup = new File(memcg + "/" + bgCGroup + "/cgroup.procs");

        if (memcg.isEmpty() || !(fTopProcs.exists() && fBgProcs.exists())) {
            System.out.println("The kernel does not support this feature!");
            return;
        }
        if (!(fgGroup.exists() && bgGroup.exists())) {
            System.out.println("The CGroup has not been created!");
            return;
        }

        String currentTopProcs = "";
        while (true) {
            String topProcsStr = readAllText(fTopProcs);
            if (!topProcsStr.equals(currentTopProcs)) {
                currentTopProcs = topProcsStr;

                String[] topProcs = readAllText(fTopProcs).split("\n");
                String[] bgProcs = readAllText(fBgProcs).split("\n");
                for (String pid : topProcs) {
                    if (include(bgProcs, pid) && getOomADJ(pid) > 0) {
                        writePid(pid, bgGroup);
                    } else {
                        writePid(pid, fgGroup);
                    }
                }
                for (String pid : bgProcs) {
                    if (!include(topProcs, pid)) {
                        writePid(pid, bgGroup);
                    }
                }
            }
            Thread.sleep(5000);
        }

        /*
        # CGroup
        if [[ -d /sys/fs/cgroup/memory ]]; then
          memcg="/sys/fs/cgroup/memory"
        elif [[ -d /dev/memcg ]]; then
          memcg="/dev/memcg"
        fi

        top_path=/dev/cpuset/top-app/cgroup.procs
        bg_path=/dev/cpuset/background/cgroup.procs
        bg_group=$memcg/scene_bg/cgroup.procs
        fg_group=$memcg/scene_fg/cgroup.procs

        if [[ ! -e $top_path ]] || [[ ! -e $bg_path ]] || [[ ! -e $fg_group ]] || [[ ! -e $bg_group ]]; then
          return
        fi

        auto_set() {
          bg_procs=$(cat $bg_path)
          fg_procs=$(cat $top_path)

          echo "$fg_procs" | while read line ; do
            if [[ $(echo "$bg_procs" | grep -E "^$line\$") == '' ]]; then
              echo $line > $fg_group
            else
              oom_adj=$(cat /proc/$line/oom_adj)
              echo $line
              if [[ $oom_adj -gt 0 ]]; then
                echo $line > $bg_group
              fi
            fi
          done

          echo "$bg_procs" | while read line ; do
            if [[ $(echo "$fg_procs" | grep -E "^$line\$") == '' ]]; then
              echo $line > $bg_group
            fi
          done
        }


        last_procs=''
        while true
        do
          current=$(cat $top_path)
          if [[ "$current" != "$last_procs" ]]; then
            last_procs=”$current”
            auto_set
          fi
          sleep 5
        done



        */
    }
}
