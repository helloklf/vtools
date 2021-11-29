package com.omarea.library.shell;

import android.annotation.SuppressLint;

import com.omarea.common.shell.KernelProrp;

import java.util.HashMap;

/**
 * CPU负载计算器
 */
public class CpuLoadUtils {
    private static String lastCpuState = "";
    private static HashMap<Integer, Double> lastCpuStateMap;
    private static String lastCpuStateSum = "";
    private static Long lastCpuStateTime;

    public CpuLoadUtils() {
        lastCpuState = KernelProrp.INSTANCE.getProp("/proc/stat", "^cpu");
        lastCpuStateSum = lastCpuState;
    }

    private int getCpuIndex(String[] cols) {
        int cpuIndex;
        if (cols[0].equals("cpu")) {
            cpuIndex = -1;
        } else {
            cpuIndex = Integer.parseInt(cols[0].substring(3));
        }
        return cpuIndex;
    }

    private long cpuTotalTime(String[] cols) {
        long totalTime = 0;
        for (int i = 1; i < cols.length; i++) {
            totalTime += Long.parseLong(cols[i]);
        }
        return totalTime;
    }

    private long cpuIdelTime(String[] cols) {
        return Long.parseLong(cols[4]);
    }

    // 返回数据如： { -1: 50.5, 0: 80.9, 1: 75.5 ... },  -1 表示所有核心的整体利用率，0~7则为正常的cpu序号
    public HashMap<Integer, Double> getCpuLoad() {
        if (lastCpuStateMap != null && System.currentTimeMillis() - lastCpuStateTime < 500) {
            return lastCpuStateMap;
        }

        @SuppressLint("UseSparseArrays") HashMap<Integer, Double> loads = new HashMap<>();
        String times = KernelProrp.INSTANCE.getProp("/proc/stat", "^cpu");
        if (!times.equals("error") && times.startsWith("cpu")) {
            try {
                if (lastCpuState.isEmpty()) {
                    lastCpuState = times;
                    Thread.sleep(100);
                    return getCpuLoad();
                } else {
                    String[] curTick = times.split("\n");
                    String[] prevTick = lastCpuState.split("\n");

                    for (String cpuCurrentTime : curTick) {
                        String[] cols1 = cpuCurrentTime.replaceAll(" {2}", " ").split(" ");
                        String[] cols0 = null;
                        // 根据前缀匹配上一个时段的cpu时间数据
                        for (String cpu : prevTick) {
                            // startsWith条件必须加个空格，因为搜索cpu的时候 "cpu0 ..."、"cpu1 ..."等都会匹配
                            if (cpu.startsWith(cols1[0] + " ")) {
                                cols0 = cpu.replaceAll(" {2}", " ").split(" ");
                                break;
                            }
                        }
                        if (cols0 != null && cols0.length != 0) {
                            long total1 = cpuTotalTime(cols1);
                            long idel1 = cpuIdelTime(cols1);
                            long total0 = cpuTotalTime(cols0);
                            long idel0 = cpuIdelTime(cols0);
                            long timePoor = total1 - total0;
                            // 如果CPU时长是0，那就是离线咯
                            if (timePoor == 0) {
                                loads.put(getCpuIndex(cols1), 0d);
                            } else {
                                long idelTimePoor = idel1 - idel0;
                                if (idelTimePoor < 1) {
                                    loads.put(getCpuIndex(cols1), 100d);
                                } else {
                                    double load = (100 - (idelTimePoor * 100.0 / timePoor));
                                    loads.put(getCpuIndex(cols1), load);
                                }
                            }
                        } else {
                            loads.put(getCpuIndex(cols1), 0d);
                        }
                    }
                    lastCpuState = times;
                    // 缓存状态以优化性能
                    lastCpuStateTime = System.currentTimeMillis();
                    lastCpuStateMap = loads;
                    return loads;
                }
            } catch (Exception ex) {
                return loads;
            }
        } else {
            return loads;
        }
    }

    public Double getCpuLoadSum() {
        if (lastCpuStateMap != null && System.currentTimeMillis() - lastCpuStateTime < 500 && lastCpuStateMap.containsKey(-1)) {
            return lastCpuStateMap.get(-1);
        }

        String times = KernelProrp.INSTANCE.getProp("/proc/stat", "^cpu ");
        if (!times.equals("error") && times.startsWith("cpu")) {
            try {
                if (lastCpuStateSum.isEmpty()) {
                    lastCpuStateSum = times;
                    Thread.sleep(100);
                    return getCpuLoadSum();
                } else {
                    String[] curTick = times.split("\n");
                    String[] prevTick = lastCpuStateSum.split("\n");

                    for (String cpuCurrentTime : curTick) {
                        String[] cols1 = cpuCurrentTime.replaceAll(" {2}", " ").split(" ");
                        if (cols1[0].trim().equals("cpu")) {
                            String[] cols0;
                            // 根据前缀匹配上一个时段的cpu时间数据
                            for (String cpu : prevTick) {
                                // startsWith条件必须加个空格，因为搜索cpu的时候 "cpu0 ..."、"cpu1 ..."等都会匹配
                                if (cpu.startsWith("cpu ")) {
                                    lastCpuStateSum = times;
                                    cols0 = cpu.replaceAll(" {2}", " ").split(" ");
                                    long total1 = cpuTotalTime(cols1);
                                    long idel1 = cpuIdelTime(cols1);
                                    long total0 = cpuTotalTime(cols0);
                                    long idel0 = cpuIdelTime(cols0);
                                    long timePoor = total1 - total0;
                                    // 如果CPU时长是0，那就是离线咯
                                    if (timePoor == 0) {
                                        return 0d;
                                    } else {
                                        long idelTimePoor = idel1 - idel0;
                                        if (idelTimePoor < 1) {
                                            return 100d;
                                        } else {
                                            return (100 - (idelTimePoor * 100.0 / timePoor));
                                        }
                                    }
                                }
                            }
                            return 0d;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return -1d;
    }
}
