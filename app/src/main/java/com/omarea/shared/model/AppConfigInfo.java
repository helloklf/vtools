package com.omarea.shared.model;

public class AppConfigInfo {
    public String packageName;

    // AppConfig
    public boolean aloneLight = false;
    public boolean disNotice = false;
    public boolean disButton = false;
    public boolean disBackgroundRun = false;
    public boolean gpsOn = false;

    // Xposed
    public int dpi = -1;
    public boolean excludeRecent = false;
    public boolean smoothScroll = false;
}
