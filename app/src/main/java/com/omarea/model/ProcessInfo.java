package com.omarea.model;

public class ProcessInfo {
    public int pid;
    public String name;
    public float cpu;
    public long rss;
    public long shr;
    public long uss;

    public String user;
    public String command;
    public String cmdline;

    public String friendlyName = "";

    public String cpuSet;
    public String cGroup;
    public String oomAdj;
    public String oomScore;
}
