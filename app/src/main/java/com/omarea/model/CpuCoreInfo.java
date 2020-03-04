package com.omarea.model;

public class CpuCoreInfo {
    public CpuCoreInfo(int coreIndex) {
        this.coreIndex = coreIndex;
    }

    public int coreIndex;
    public String minFreq;
    public String maxFreq;
    public String currentFreq;
    public double loadRatio;
}
