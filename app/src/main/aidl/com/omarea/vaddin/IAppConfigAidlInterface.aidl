// IAppConfigAidlInterface.aidl
package com.omarea.vaddin;

// Declare any non-default types here with import statements

interface IAppConfigAidlInterface {
    int getVersion();
    boolean updateAppConfig(String packageName, int dpi, boolean excludeRecent, boolean smoothScroll);
    String getAppConfig(String packageName);
    boolean setBooleanValue(String field, boolean value);
    boolean setStringValue(String field, String value);
    boolean setIntValue(String field, int value);
    boolean getBooleanValue(String field, boolean defValue);
    int getIntValue(String field, int defValue);
    String getStringValue(String field, String defValue);
}
