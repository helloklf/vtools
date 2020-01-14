package com.omarea.gesture.util;

import android.content.Context;

import java.util.ArrayList;

public class Recents {
    private final ArrayList<String> recents = new ArrayList<>();
    private final int sizeLimit = 60;
    // TODO:关闭辅助服务时清理以下数据
    // 已经确保可以打开的应用
    public ArrayList<String> whiteList = new ArrayList<>();
    // 已经可以肯定不是可以打开的应用
    public ArrayList<String> blackList = new ArrayList<String>() {
    };

    // 忽略的应用
    public ArrayList<String> ignoreApps = null;
    public ArrayList<String> inputMethods = null;
    private int index = -1;
    private String currentTop = "";

    public void clear() {
        synchronized (recents) {
            recents.clear();
            currentTop = "";
        }
    }

    public boolean addRecent(String packageName) {
        if (currentTop.equals(packageName)) {
            return false;
        }

        synchronized (recents) {
            int searchResult = recents.indexOf(packageName);
            if (searchResult > -1) {
                recents.remove(searchResult);
            }
            if (recents.size() >= sizeLimit) {
                recents.remove(0);
                if (index >= recents.size()) {
                    index = recents.size() - 1;
                }
            }

            if (index > -1) {
                recents.add(index, packageName);
            } else {
                recents.add(packageName);
            }
            index = recents.indexOf(packageName);
            currentTop = packageName;
        }
        return true;
    }

    public void setRecents(ArrayList<String> items, Context context) {
        synchronized (recents) {
            /*
            if (recents.size() < 4) {
                recents.clear();
                for (String packageName : items) {
                    if (
                            whiteList.indexOf(packageName) > -1 ||
                            (blackList.indexOf(packageName) < 0 && ignoreApps.indexOf(packageName) < 0)
                    ) {
                        recents.add(packageName);
                    }
                }
                index = recents.indexOf(currentTop);
            } else {
                ArrayList<String> lostedItems = new ArrayList<>();
                for (String recent : recents) {
                    if (items.indexOf(recent) < 0) {
                        lostedItems.add(recent);
                    }
                }
                recents.removeAll(lostedItems);
                index = recents.indexOf(currentTop);
            }
            */

            ArrayList<String> lostedItems = new ArrayList<>();
            for (String recent : recents) {
                if (items.indexOf(recent) < 0) {
                    lostedItems.add(recent);
                }
            }
            recents.removeAll(lostedItems);
            index = recents.indexOf(currentTop);
        }
    }

    public int getIndex(String packageName) {
        return recents.indexOf(packageName);
    }

    public void setIndex(int to) {
        if (index < recents.size()) {
            index = to;
        }
    }

    public String getCurrent() {
        return currentTop;
    }

    public String movePrevious() {
        synchronized (recents) {
            if (index > 0) {
                index -= 1;
                return recents.get(index);
            } else if (recents.size() > 0) {
                int size = recents.size();
                index = size - 1;
                return recents.get(index);
            } else {
                return null;
            }
        }
    }

    public String moveNext() {
        synchronized (recents) {
            if (index < recents.size() - 1) {
                index += 1;
                return recents.get(index);
            } else if (recents.size() > 0) {
                index = 0;
                return recents.get(0);
            } else {
                return null;
            }
        }
    }
}
