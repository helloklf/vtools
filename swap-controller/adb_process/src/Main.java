import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class Main {
    private static final double criticalKill = 0.12;
    private static double critical = 0.17;
    private static double high = 0.23;
    private static double middle = 0.25;

    private static final File fTopProcs = new File("/dev/cpuset/top-app/cgroup.procs");
    private static final File fBgProcs = new File("/dev/cpuset/background/cgroup.procs");
    private static final File fFgProcs = new File("/dev/cpuset/foreground/cgroup.procs");

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

    private static long memInfoRowValue (String row) {
        return Integer.parseInt(row.substring(row.indexOf(":") + 1, row.lastIndexOf(" ")).trim());
    }

    private static MemInfo getMemInfo() {
        String[] memInfoRows = readAllText(new File("/proc/meminfo")).split("\n");
        MemInfo memInfo = new MemInfo();
        for (String row : memInfoRows) {
            if (row.startsWith("SwapFree")) {
                memInfo.SwapFree = memInfoRowValue(row);
            } else if (row.startsWith("MemAvailable")) {
                memInfo.MemAvailable = memInfoRowValue(row);
            } else if (row.startsWith("SwapCached")) {
                memInfo.SwapCached = memInfoRowValue(row);
            } else if (row.startsWith("MemTotal")) {
                memInfo.MemTotal = memInfoRowValue(row);
            }
        }
        return memInfo;
    }

    static class MemInfo {
        // Swap空想
        public long SwapFree;
        // 总内存
        public long MemTotal;
        // 可用内存
        public long MemAvailable;
        public long SwapCached;

        // 内存空闲比例
        public double getMemFreeRatio() {
            if (MemTotal > 0 && MemAvailable > 0) {
                return (0.0 + MemAvailable + SwapCached) / MemTotal;
            }
            return 0;
        }

        // 内存（绝对）空闲比例，忽略SwapCached大小
        public double getMemAbsFreeRatio() {
            if (MemTotal > 0 && MemAvailable > 0) {
                return (0.0 + MemAvailable) / MemTotal;
            }
            return 0;
        }

        // SwapCached 占内存的比例
        public double getSwapCacheRatio() {
            if (MemTotal > 0 && SwapCached > 0) {
                return (0.0 + SwapCached) / MemTotal;
            }
            return 0;
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
        private final File kernelReference = new File("/sys/block/zram0/writeback");

        private void writeIdle() {
            writeFile("idle", kernelReference);
        }
    }

    static class MemoryWatch extends Thread {
        private final File idleGroup;
        public MemoryWatch(File idleGroup) {
            if (idleGroup != null && idleGroup.exists()) {
                this.idleGroup = idleGroup;
            } else {
                this.idleGroup = null;
            }
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
                        } else if (memFreeRatio > 0.25) {
                            reclaimReason.wait(30000);
                        } else if (memFreeRatio > 0.1) {
                            reclaimReason.wait(20000);
                        } else {
                            reclaimReason.wait(10000);
                        }
                    } catch (InterruptedException ignored) {
                    }
                }

                this.killProcess();
                memFreeRatio = getMemFreeRatio();

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
                                // method = "all"; // 实际性能表现不好
                                method = "file";
                            } else {
                                method = "file";
                                // 间隔120秒执行过Reclaim，且回收需求不是发生在前台应用切换 跳过
                                if (reclaimReason.reason != ReclaimReason.REASON_APP_WATCH &&
                                        System.currentTimeMillis() - lastReclaim < 120000) {
                                    continue;
                                }
                            }

                            List<LinuxProcess> processArr = this.getSortedProcess();
                            for (LinuxProcess process : processArr) {
                                // System.out.println(">>" + process.pid + "|" + process.oomAdj + "|" + method);
                                if (reclaimProcess(process.pid, method)) {
                                    break;
                                }
                            }

                            lastReclaim = System.currentTimeMillis();
                        }
                    }
                }
            }
        }

        // 杀死一些进程来缓解严重的内存不足
        private void killProcess() {
            MemInfo memInfo = getMemInfo();

            // 内存负载是否已经很危险：可活动内存不足且Swap剩余空间很低，或空闲内存是否已经到临界值
            boolean danger = (
                (memInfo.getMemFreeRatio() <= criticalKill && memInfo.SwapFree < 204800) || memInfo.getMemAbsFreeRatio() < 0.055
            );

            if (danger) {
                System.out.println("#KillProcess Killing Start");
                System.out.println(" memInfo.getMemFreeRatio(): " + memInfo.getMemFreeRatio());
                System.out.println(" memInfo.getMemAbsFreeRatio(): " + memInfo.getMemAbsFreeRatio());

                List<LinuxProcess> processes = this.getSortedProcess();
                for (LinuxProcess process : processes) {
                    // System.out.println(">>" + process.pid + "|" + process.oomAdj + "|" + method);
                    if (killProcess(process.pid)) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception ignored) {
                        }
                        break;
                    }
                }
            } else {
                System.out.println("#KillProcess PreCheck");
                System.out.println(" memInfo.getMemFreeRatio(): " + memInfo.getMemFreeRatio());
                System.out.println(" memInfo.getMemAbsFreeRatio(): " + memInfo.getMemAbsFreeRatio());
            }
        }

        // 杀死指定进程 并检查内存是否充足（无需继续杀死进程）
        private boolean killProcess(String pid) {
            try {
                Runtime.getRuntime().exec("kill -9 " + pid);
            } catch (Exception ignored) {}
            try {
                Thread.sleep(50);
            } catch (Exception ignored){}

            MemInfo memInfo = getMemInfo();
            return memInfo.getMemFreeRatio() >= high || (memInfo.getMemAbsFreeRatio() >= criticalKill && memInfo.SwapFree >= 409600);
        }

        // 获取根据oomAdj大小倒叙排列的进程列表
        private List<LinuxProcess> getSortedProcess() {
            String[] recyclable = readAllText(idleGroup).split("\n");
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

            return processArr;
        }

        // 回收指定进程，并检验内存是否充足或无需继续回收
        private boolean reclaimProcess(String pid, String method) {
            reclaimByPID(pid, method);

            try {
                Thread.sleep(50);
            } catch (Exception ex){}

            MemInfo memInfo = getMemInfo();
            // 如果Swap空闲不足、内存已经充足、SwapCached占用很高 停止回收
            if (memInfo.SwapFree < 100 || memInfo.getMemFreeRatio() >= middle || memInfo.getSwapCacheRatio() > 0.07) {
                return true;
            }

            return false;
        }
    }

    static class MemoryWatchBasic extends Thread {
        @Override
        public void run() {
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
                        } else if (memFreeRatio > 0.25) {
                            reclaimReason.wait(30000);
                        } else if (memFreeRatio > 0.1) {
                            reclaimReason.wait(20000);
                        } else {
                            reclaimReason.wait(10000);
                        }
                    } catch (InterruptedException ignored) {
                    }
                }

                this.killProcess();
            }
        }

        // 杀死一些进程来缓解严重的内存不足
        private void killProcess() {
            MemInfo memInfo = getMemInfo();

            // 内存负载是否已经很危险：可活动内存不足且Swap剩余空间很低，或空闲内存是否已经到临界值
            boolean danger = (
                    (memInfo.getMemFreeRatio() <= criticalKill && memInfo.SwapFree < 204800) || memInfo.getMemAbsFreeRatio() < 0.055
            );

            if (danger) {
                System.out.println("#KillProcess Killing Start");
                System.out.println(" memInfo.getMemFreeRatio(): " + memInfo.getMemFreeRatio());
                System.out.println(" memInfo.getMemAbsFreeRatio(): " + memInfo.getMemAbsFreeRatio());

                List<LinuxProcess> processes = this.getSortedProcess();
                for (LinuxProcess process : processes) {
                    // System.out.println(">>" + process.pid + "|" + process.oomAdj + "|" + method);
                    if (killProcess(process.pid)) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception ignored) {
                        }
                        break;
                    }
                }
            } else {
                System.out.println("#KillProcess PreCheck");
                System.out.println(" memInfo.getMemFreeRatio(): " + memInfo.getMemFreeRatio());
                System.out.println(" memInfo.getMemAbsFreeRatio(): " + memInfo.getMemAbsFreeRatio());
            }
        }

        // 杀死指定进程 并检查内存是否充足（无需继续杀死进程）
        private boolean killProcess(String pid) {
            try {
                Runtime.getRuntime().exec("kill -9 " + pid);
            } catch (Exception ignored) {}
            try {
                Thread.sleep(50);
            } catch (Exception ignored){}

            MemInfo memInfo = getMemInfo();
            return memInfo.getMemFreeRatio() >= high || (memInfo.getMemAbsFreeRatio() >= criticalKill && memInfo.SwapFree >= 409600);
        }

        // 获取空闲(后台)进程
        private List<String> getIdleProcess() {
            String[] topProcs = readAllText(fTopProcs).split("\n");
            String[] bgProcs = readAllText(fBgProcs).split("\n");
            String[] fgProcs = readAllText(fFgProcs).split("\n");
            List<String> idleProcess = new ArrayList<>();

            // top
            for (String pid : topProcs) {
                if (include(bgProcs, pid) && getOomADJ(pid) > 1) {
                    idleProcess.add(pid);
                }
            }
            // background
            for (String pid : bgProcs) {
                if (!include(topProcs, pid) && getOomADJ(pid) > 1) {
                    idleProcess.add(pid);
                }
            }
            // foreground
            for (String pid: fgProcs) {
                if (getOomADJ(pid) > 1) {
                    idleProcess.add(pid);
                }
            }

            return idleProcess;
        }

        // 获取根据oomAdj大小倒叙排列的进程列表
        private List<LinuxProcess> getSortedProcess() {
            List<String> recyclable = getIdleProcess(); // readAllText(fBgProcs).split("\n");
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

            return processArr;
        }
    }

    static class WatchForeground extends Thread {
        private final File idleGroup;
        public WatchForeground(File idleGroup) {
            if (idleGroup != null && idleGroup.exists()) {
                this.idleGroup = idleGroup;
            } else {
                this.idleGroup = null;
            }
        }

        @Override
        public void run() {
            String currentFProcs = "";
            if (fFgProcs.exists() && idleGroup != null) {
                while (true) {
                    String str = readAllText(fFgProcs);
                    if (!str.equals(currentFProcs)) {
                        currentFProcs = str;
                        String[] fgProcs = currentFProcs.split("\n");
                        // foreground
                        for (String pid: fgProcs) {
                            // if (getOomADJ(pid) > 0) {
                            if (getOomADJ(pid) > 1) {
                                writeFile(pid, idleGroup);
                            }
                        }
                        double memFreeRatio = getMemFreeRatio();
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

    // 检测top-app
    static void startAppWatch(File activeGroup, File idleGroup) throws InterruptedException {
        String currentTopProcs = "";
        long lastChange = 0L;
        while (true) {
            String topProcsStr = readAllText(fTopProcs);
            if (!topProcsStr.equals(currentTopProcs)) {
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
                if (memFreeRatio <= middle) {
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

    // 检测top-app
    static void startAppWatchBasic() throws InterruptedException {
        String currentTopProcs = "";
        long lastChange = 0L;
        while (true) {
            String topProcsStr = readAllText(fTopProcs);
            if (!topProcsStr.equals(currentTopProcs)) {
                currentTopProcs = topProcsStr;
                double memFreeRatio = getMemFreeRatio();
                // reclaim
                if (memFreeRatio <= middle) {
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

    // 是否已经内存不足
    private static boolean isLowMemory (double memFreeRatio) {
        return memFreeRatio <= high || (memFreeRatio <= middle && reclaimReason.reason == ReclaimReason.REASON_APP_WATCH);
    }

    private static void preConfig(String mode) {
        switch (mode) {
            case "basic":
            case "passive": {
                critical = 0.17;
                high = 0.23;
                middle = 0.25;
                break;
            }
            case "force": {
                critical = 0.23;
                high = 0.27;
                middle = 0.30;
                break;
            }
            case "active": {
                critical = 0.20;
                high = 0.25;
                middle = 0.28;
                break;
            }
            case "lazy": {
                critical = 0.14;
                high = 0.16;
                middle = 0.20;
                break;
            }
            default: {
                critical = 0.17;
                high = 0.23;
                middle = 0.25;
                break;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        boolean cpusetSupported = fTopProcs.exists() && fFgProcs.exists() && fBgProcs.exists();
        if (!cpusetSupported) {
            System.out.println("cpuset: The kernel does not support this feature!");
            return;
        }

        String cgroupReclaim = args.length > 0 ? args[0].trim() : "basic";
        preConfig(cgroupReclaim);

        String memcg = "";
        if (new File("/sys/fs/cgroup/memory").exists()) {
            memcg = "/sys/fs/cgroup/memory";
        } else if (new File("/dev/memcg").exists()) {
            memcg = "/dev/memcg";
        }

        File activeGroup = new File(memcg + "/scene_active/cgroup.procs");
        File idleGroup = new File(memcg + "/scene_idle/cgroup.procs");

        // 是否启用memcg
        boolean memcgEnabled;
        // 是否支持进程reclaim
        boolean reclaimSupported = false;
        if (memcg.isEmpty() || !(fTopProcs.exists() && fBgProcs.exists())) {
            System.out.println("memcg: The kernel does not support this feature!");
            memcgEnabled = false;
        } else if (!(activeGroup.exists() && idleGroup.exists())) {
            System.out.println("memcg: The CGroup has not been created!");
            memcgEnabled = false;
        } else {
            memcgEnabled = true;
            reclaimSupported = new File("/proc/1/reclaim").exists();
        }

        if (memcgEnabled && reclaimSupported) {
            new MemoryWatch(idleGroup).start();
            new WatchForeground(idleGroup).start();
            startAppWatch(activeGroup, idleGroup);
        } else {
            new MemoryWatchBasic().start();
            startAppWatchBasic();
        }
    }
}
