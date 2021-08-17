import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

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

    private static String getCpuSet(String pid) {
        String adj = readAllText(new File("/proc/" + pid + "/cpuset"));
        if (!adj.isEmpty()) {
            return adj;
        }
        return "/";
    }

    private static boolean include(String[] arr, String value) {
        for (String s : arr) {
            if (s.equals(value)) {
                return true;
            }
        }
        return false;
    }

    // method (file\anon\all)
    private static void reclaimPid(String pid, String method) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File("/proc/" + pid + "/reclaim"));
            byte[] bytes = method.getBytes();
            fileOutputStream.write(bytes, 0, bytes.length);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException ex) {
            System.out.println("WritePid Fail: " + ex.getMessage());
        }
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

    // 提取 /proc/meminfo 里某一行的数值，
    // 例： [MemFree:          138828 kB] => [138828]
    private static int getMemInfoRowKB(String row) {
        return Integer.parseInt(
                row.substring(
                        row.indexOf(":") + 1,
                        row.lastIndexOf(" ")
                ).trim()
        );
    }

    // 内存空闲比例
    private static double getMemFreeRatio() {
        String[] memInfos = readAllText(new File("/proc/meminfo")).split("\n");
        double total = 0;
        double free = 0;
        double swapCached = 0;
        for (String row : memInfos) {
            if (row.startsWith("MemTotal")) {
                total = getMemInfoRowKB(row);
            } else if (row.startsWith("MemAvailable")) {
                free = getMemInfoRowKB(row);
            } else if (row.startsWith("SwapCached")) {
                swapCached = getMemInfoRowKB(row);
            }
        }
        if (total > 0 && free > 0) {
            return (free + swapCached) / total;
        }
        return 0;
    }

    // 获取空闲SWAP
    private static int getSwapFreeMB() {
        String[] memInfos = readAllText(new File("/proc/meminfo")).split("\n");
        for (String row : memInfos) {
            if (row.startsWith("SwapFree")) {
                String value = row.substring(row.indexOf(":") + 1, row.lastIndexOf(" "));
                return Integer.parseInt(value.trim()) / 1024;
            }
        }
        return 0;
    }

    static class WatchForeground extends Thread {
        private File bgGroup;
        public WatchForeground(File bgGroup) {
            this.bgGroup = bgGroup;
        }

        @Override
        public void run() {
            File fFgProcs = new File("/dev/cpuset/foreground/cgroup.procs");
            String currentFProcs = "";
            if (fFgProcs.exists()) {
                while (true) {
                    String str = readAllText(fFgProcs);
                    if (!str.equals(currentFProcs)) {
                        currentFProcs = str;
                        String[] fgProcs = currentFProcs.split("\n");
                        ArrayList<String> recyclable = new ArrayList<>();
                        double memFreeRatio = getMemFreeRatio();
                        // foreground
                        for (String pid: fgProcs) {
                            // if (getOomADJ(pid) > 0) {
                            if (getOomADJ(pid) > 1) {
                                writePid(pid, bgGroup);
                                recyclable.add(pid);
                            }
                        }
                        // reclaim
                        if (memFreeRatio < 0.25) {
                            synchronized (threadSync) {
                                threadSync.notifyAll();
                            }
                        }
                    }

                    try {
                        Thread.sleep(30000);
                    } catch (Exception ignored){}
                }
            }
        }
    }

    static final Object threadSync = new Object();

    static class MemoryWatch extends Thread {
        private File bgGroup;
        public MemoryWatch(File bgGroup) {
            this.bgGroup = bgGroup;
        }

        @Override
        public void run() {
            long lastReclaim = 0L;
            while (true) {
                double memFreeRatio = getMemFreeRatio();

                synchronized (threadSync) {
                    try {
                        if (memFreeRatio >= 0.6) {
                            threadSync.wait(180000);
                        } else if (memFreeRatio >= 0.5) {
                            threadSync.wait(120000);
                        } else if (memFreeRatio > 0.4) {
                            threadSync.wait(60000);
                        } else {
                            threadSync.wait(30000);
                        }
                    } catch (InterruptedException ignored) {
                    }
                }

                memFreeRatio = getMemFreeRatio();
                int swapFree = getSwapFreeMB();
                if (swapFree >= 300 && memFreeRatio < 0.22) {
                    String method = "";
                    if (memFreeRatio < 0.15 && swapFree > 500) {
                        method = "all";
                    } else {
                        method = "file";
                        // 间隔120秒执行过Reclaim 跳过
                        if (System.currentTimeMillis() - lastReclaim < 120000) {
                            continue;
                        }
                    }

                    String[] recyclable = readAllText(bgGroup).split("\n");
                    for (String pid: recyclable) {
                        reclaimPid(pid, method);
                    }
                    lastReclaim = System.currentTimeMillis();
                }

            }
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

        new WatchForeground(bgGroup).start();
        new MemoryWatch(bgGroup).start();

        String currentTopProcs = "";
        while (true) {
            String topProcsStr = readAllText(fTopProcs);
            if (!topProcsStr.equals(currentTopProcs)) {
                currentTopProcs = topProcsStr;

                String[] topProcs = readAllText(fTopProcs).split("\n");
                String[] bgProcs = readAllText(fBgProcs).split("\n");
                double memFreeRatio = getMemFreeRatio();
                ArrayList<String> recyclable = new ArrayList<>();
                // top
                for (String pid : topProcs) {
                    if (include(bgProcs, pid) && getOomADJ(pid) > 1) {
                        writePid(pid, bgGroup);
                        recyclable.add(pid);
                    } else {
                        writePid(pid, fgGroup);
                    }
                }
                // background
                for (String pid : bgProcs) {
                    if (!include(topProcs, pid) && getOomADJ(pid) > 1) {
                        writePid(pid, bgGroup);
                        recyclable.add(pid);
                    }
                }
                // reclaim
                if (memFreeRatio < 0.25) {
                    synchronized (threadSync) {
                        threadSync.notifyAll();
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
