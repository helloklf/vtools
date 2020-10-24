package com.omarea.model;

public class CpuCoreInfo {
    public int coreIndex;
    public String minFreq;
    public String maxFreq;
    public String currentFreq;
    public double loadRatio;
    public CpuCoreInfo(int coreIndex) {
        this.coreIndex = coreIndex;
    }
}
