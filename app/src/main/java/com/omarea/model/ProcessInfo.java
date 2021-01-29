package com.omarea.model;

public class ProcessInfo {
    public int pid;
    public String name;
    public float cpu;
    public String getState () {
        switch (state) {
            case "R": return "R (running)";
            case "S": return "S (sleeping)";
            case "D": return "D (device I/O)";
            case "T": return "T (stopped)";
            case "t": return "t (trace stop)";
            case "X": return "X (dead)";
            case "Z": return "Z (zombie)";
            case "P": return "P (parked)";
            case "I": return "I (idle)";
            case "x": return "x (dead)";
            case "K": return "K (wakekill)";
            case "W": return "W (waking)";
            default: return "Unknown";
        }
    }

    /*
    Process state:
          R (running) S (sleeping) D (device I/O) T (stopped)  t (trace stop)
          X (dead)    Z (zombie)   P (parked)     I (idle)
          Also between Linux 2.6.33 and 3.13:
          x (dead)    K (wakekill) W (waking)
    */
    public float getCpu () {
        /*
        switch (state) {
            // case "S":
            case "T":
            case "t":
            case "X":
            case "P":
            case "I":
            case "x": {
                return 0f;
            }
            default: {
                return cpu;
            }
        }
       */
        return cpu;
    }
    public long res;
    public long rss;
    public long mem;
    public long swap;
    public String state = "";
    public String user;
    public String command = "";
    public String cmdline = "";

    public String friendlyName = "";

    public String cpuSet;
    public String cGroup;
    public String oomAdj;
    public String oomScore;
    public String oomScoreAdj;
}
