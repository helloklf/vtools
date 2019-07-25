package com.omarea.model;

import java.io.Serializable;
import java.util.HashMap;

public class CpuClusterStatus implements Serializable {
    public String min_freq = "";
    public String max_freq = "";
    public String governor = "";
    public HashMap<String, String> governor_params = null;
}