package com.omarea;

public class AppConfigInfo {
    public String packageName;

    // AppConfig
    public boolean aloneLight = false;
    public int aloneLightValue = -1;
    public boolean disNotice = false;
    public boolean disButton = false;
    public boolean disBackgroundRun = false;
    public boolean gpsOn = false;

    // Xposed
    public int dpi = -1;
    public boolean excludeRecent = false;
    public boolean smoothScroll = false;
}
