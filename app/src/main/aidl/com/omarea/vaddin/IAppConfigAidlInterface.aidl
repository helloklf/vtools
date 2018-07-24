// IAppConfigAidlInterface.aidl
package com.omarea.vaddin;

// Declare any non-default types here with import statements

interface IAppConfigAidlInterface {
    int getVersion();
    boolean updateAppConfig(String packageName, int dpi, boolean excludeRecent, boolean smoothScroll);
    String getAppConfig(String packageName);
}
