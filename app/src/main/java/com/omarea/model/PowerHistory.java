package com.omarea.model;

public class PowerHistory {
    public long io;
    public int capacity;
    public long startTime;
    public long endTime;
    public boolean screenOn;
    public boolean charging;

    @Override
    public String toString() {
        return "PowerHistory{" +
                "io=" + io +
                ", capacity=" + capacity +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", screenOn=" + screenOn +
                ", charging=" + charging +
                '}';
    }
}
