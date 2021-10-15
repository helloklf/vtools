import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class Main {
    private static double critical = 0.15;
    private static double high = 0.22;
    private static double middle = 0.25;
    private static double prekill = 0.27;

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
    private static void reclaimByPID(String pid, String method) {
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

    private static void writeFile(String content, File file) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte[] bytes = content.getBytes();
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
        String[] memArr = readAllText(new File("/proc/meminfo")).split("\n");
        double total = 0;
        double free = 0;
        double swapCached = 0;
        for (String row : memArr) {
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
        private final File bgGroup;
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
                        double memFreeRatio = getMemFreeRatio();
                        // foreground
                        for (String pid: fgProcs) {
                            // if (getOomADJ(pid) > 0) {
                            if (getOomADJ(pid) > 1) {
                                writeFile(pid, bgGroup);
                            }
                        }
                        // reclaim
                        if (memFreeRatio <= high) {
                            synchronized (reclaimReason) {
                                reclaimReason.reason = ReclaimReason.REASON_FOREGROUND_WATCH;
                                reclaimReason.notifyAll();
                            }
                        }
                    }

                    try {
                        Thread.sleep(60000);
                    } catch (Exception ignored){}
                }
            }
        }
    }

    static class ReclaimReason {
        private static final int REASON_MEMORY_WATCH = 1;
        private static final int REASON_APP_WATCH = 2;
        private static final int REASON_FOREGROUND_WATCH = 3;

        // 回收内存的原因（基于不同的回收原因，会有不同的力度）
        int reason = 1;
    }

    private static final ReclaimReason reclaimReason = new ReclaimReason();

    static class LinuxProcess {
        String pid;
        int oomAdj;
    }

    static class WriteBack {
        private File kernelReference = new File("/sys/block/zram0/writeback");

        private void writeIdle() {
            writeFile("idle", kernelReference);
        }
    }

    static class ProcKiller extends Thread {
        private static byte[] cmds = ("sh /system/bin/scene_task_min.sh\n\nexit 0\nexit 0\n").getBytes();

        @Override
        public void run() {
            try {
                Process process = Runtime.getRuntime().exec("sh");
                OutputStream outputStream = process.getOutputStream();
                outputStream.write(cmds);
                outputStream.flush();
                outputStream.close();
                process.waitFor();
            } catch (Exception ignored) {}
        }
    }

    static class MemoryWatch extends Thread {
        private final File bgGroup;
        // 是否根据oomAdj对进程排序
        private final boolean sortProcess = true;
        MemoryWatch(File bgGroup) {
            this.bgGroup = bgGroup;
        }

        @Override
        public void run() {
            // 判断ZRAM是否开启
            boolean zramEnabled = readAllText(new File("/proc/swaps")).contains("/zram0");
            boolean bdConfigured = false;
            if (zramEnabled) {
                File fileBD = new File("/sys/block/zram0/backing_dev");
                String backingDev = fileBD.exists() ? readAllText(fileBD) : "";
                bdConfigured = !(backingDev.equals("") || backingDev.equals("none"));
            }
            // 是否已配置ZRAM回写，如已配置 new WriteBack()
            WriteBack writeBack = (zramEnabled && bdConfigured) ? new WriteBack() : null;


            long lastReclaim = 0L;
            while (true) {
                double memFreeRatio = getMemFreeRatio();

                synchronized (reclaimReason) {
                    reclaimReason.reason = ReclaimReason.REASON_MEMORY_WATCH;
                    try {
                        if (memFreeRatio >= 0.6) {
                            reclaimReason.wait(180000);
                        } else if (memFreeRatio >= 0.5) {
                            reclaimReason.wait(120000);
                        } else if (memFreeRatio > 0.4) {
                            reclaimReason.wait(60000);
                        } else {
                            reclaimReason.wait(30000);
                        }
                    } catch (InterruptedException ignored) {
                    }
                }

                memFreeRatio = getMemFreeRatio();

                /*
                if (memFreeRatio < prekill) {
                    // 清理QQ、微信等应用的不重要子进程
                    Thread killer = new ProcKiller();
                    killer.start();
                    try {
                        sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
                */

                // 是否内存不足
                boolean lowMemory = isLowMemory(memFreeRatio);
                if (lowMemory) {
                    // 如果配置了 ZRAM Writeback 先触发writeback
                    if (writeBack != null) {
                        writeBack.writeIdle();
                        // writeback 介绍之后，更新空闲内存状态
                        memFreeRatio = getMemFreeRatio();
                        lowMemory = isLowMemory(memFreeRatio);
                    }

                    if (lowMemory) {
                        int swapFree = getSwapFreeMB();
                        if (swapFree >= 300) {
                            if (writeBack != null) {
                                writeBack.writeIdle();
                            }

                            String method = "";
                            if (memFreeRatio < critical && swapFree > 500) {
                                method = "all";
                            } else {
                                method = "file";
                                // 间隔120秒执行过Reclaim，且回收需求不是发生在前台应用切换 跳过
                                if (reclaimReason.reason != ReclaimReason.REASON_APP_WATCH &&
                                        System.currentTimeMillis() - lastReclaim < 120000) {
                                    continue;
                                }
                            }

                            String[] recyclable = readAllText(bgGroup).split("\n");

                            if (sortProcess) {
                                List<LinuxProcess> processArr = new ArrayList<>();
                                for (String id : recyclable) {
                                    LinuxProcess process = new LinuxProcess();
                                    process.pid = id;
                                    process.oomAdj = getOomADJ(id);
                                    processArr.add(process);
                                }
                                Collections.sort(processArr, new Comparator<LinuxProcess>() {
                                    @Override
                                    public int compare(LinuxProcess o1, LinuxProcess o2) {
                                        return o2.oomAdj - o1.oomAdj;
                                    }
                                });

                                for (LinuxProcess process : processArr) {
                                    // System.out.println(">>" + process.pid + "|" + process.oomAdj + "|" + method);
                                    reclaimByPID(process.pid, method);
                                }
                            } else {
                                for (String pid : recyclable) {
                                    reclaimByPID(pid, method);
                                }
                            }

                            lastReclaim = System.currentTimeMillis();
                        }
                    }
                }
            }
        }
    }

    // 是否已经内存不足
    private static boolean isLowMemory (double memFreeRatio) {
        return memFreeRatio <= high || (memFreeRatio <= middle && reclaimReason.reason == ReclaimReason.REASON_APP_WATCH);
    }

    public static void main(String[] args) throws InterruptedException {

        String cgroupReclaim = args.length > 0 ? args[0].trim() : "passive";
        switch (cgroupReclaim) {
            /*
            case "passive": {
                critical = 0.15;
                high = 0.22;
                middle = 0.25;
                prekill = 0.27;
                break;
            }
            */
            case "force": {
                critical = 0.23;
                high = 0.27;
                middle = 0.30;
                prekill = 0.30;
                break;
            }
            case "active": {
                critical = 0.20;
                high = 0.25;
                middle = 0.28;
                prekill = 0.30;
                break;
            }
            case "never":
            case "lazy": {
                critical = 0.14;
                high = 0.16;
                middle = 0.20;
                prekill = 0.27;
                break;
            }
            default: {
                critical = 0.17;
                high = 0.23;
                middle = 0.25;
                prekill = 0.27;
                break;
            }
        }

        String memcg = "";
        if (new File("/sys/fs/cgroup/memory").exists()) {
            memcg = "/sys/fs/cgroup/memory";
        } else if (new File("/dev/memcg").exists()) {
            memcg = "/dev/memcg";
        }

        File fTopProcs = new File("/dev/cpuset/top-app/cgroup.procs");
        File fBgProcs = new File("/dev/cpuset/background/cgroup.procs");
        File activeGroup = new File(memcg + "/scene_active/cgroup.procs");
        File idleGroup = new File(memcg + "/scene_idle/cgroup.procs");

        if (memcg.isEmpty() || !(fTopProcs.exists() && fBgProcs.exists())) {
            System.out.println("The kernel does not support this feature!");
            return;
        }
        if (!(activeGroup.exists() && idleGroup.exists())) {
            System.out.println("The CGroup has not been created!");
            return;
        }

        new WatchForeground(idleGroup).start();
        new MemoryWatch(idleGroup).start();

        String currentTopProcs = "";
        long lastChange = 0L;
        while (true) {
            String topProcsStr = readAllText(fTopProcs);
            if (!topProcsStr.equals(currentTopProcs)) {
                // 清理QQ、微信等应用的不重要子进程
                // new ProcKiller().start();

                currentTopProcs = topProcsStr;

                String[] topProcs = readAllText(fTopProcs).split("\n");
                String[] bgProcs = readAllText(fBgProcs).split("\n");
                double memFreeRatio = getMemFreeRatio();
                // top
                for (String pid : topProcs) {
                    if (include(bgProcs, pid) && getOomADJ(pid) > 1) {
                        writeFile(pid, idleGroup);
                    } else {
                        writeFile(pid, activeGroup);
                    }
                }
                // background
                for (String pid : bgProcs) {
                    if (!include(topProcs, pid) && getOomADJ(pid) > 1) {
                        writeFile(pid, idleGroup);
                    }
                }
                // reclaim
                if (memFreeRatio <= prekill) {
                    synchronized (reclaimReason) {
                        reclaimReason.reason = ReclaimReason.REASON_APP_WATCH;
                        reclaimReason.notifyAll();
                    }
                }
                lastChange = System.currentTimeMillis();
                Thread.sleep(5000);
            } else {
                long curTime = System.currentTimeMillis();
                // 超过10分钟没有切换应用，延迟轮询间隔
                if (curTime - lastChange > 600000) {
                    Thread.sleep(10000);
                } else {
                    Thread.sleep(5000);
                }
            }
        }
    }
}
