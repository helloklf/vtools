package com.omarea.shell.units;

import com.omarea.shell.KernelProrp;
import com.omarea.shell.SuDo;

import java.io.File;

/**
 * Created by Hello on 2017/11/01.
 */

public class BatteryUnit {
    //是否兼容此设备
    public boolean isSupport() {
        return new File("/sys/class/power_supply/bms/uevent").exists() || (qcSettingSuupport() || bpSetting());
    }

    //快充是否支持修改充电速度设置
    public boolean qcSettingSuupport() {
        return new File("/sys/class/power_supply/battery/constant_charge_current_max").exists();
    }

    public String getqcLimit () {
        String limit = KernelProrp.getProp("/sys/class/power_supply/battery/constant_charge_current_max");
        if (limit.length() > 3) {
            limit = limit.substring(0, limit.length() - 3) + "mA";
        } else if (limit.length() > 0) {
            try {
              if (Integer.parseInt(limit) == 0) {
                  limit = "0";
              }
            } catch (Exception ex) {

            }
        } else {
            return "?mA";
        }
        return limit;
    }

    //快充是否支持电池保护
    public boolean bpSetting() {
        return new File("/sys/class/power_supply/battery/battery_charging_enabled").exists() || new File("/sys/class/power_supply/battery/input_suspend").exists();
    }

    //获取电池信息
    public String getBatteryInfo() {
        if (new File("/sys/class/power_supply/bms/uevent").exists()) {
            String batteryInfos = KernelProrp.getProp("/sys/class/power_supply/bms/uevent");
            if (batteryInfos == null)
                batteryInfos = "";
            String[] infos = batteryInfos.split("\n");
            StringBuilder stringBuilder = new StringBuilder();
            String io = "";
            int mahLength = 0;
            for (String info : infos) {
                try {
                    String keyrowd = "";
                    if (info.startsWith(keyrowd = "POWER_SUPPLY_CHARGE_FULL=")) {
                        stringBuilder.append("充满容量 = ");
                        stringBuilder.append(info.substring(keyrowd.length(), keyrowd.length() + 4));
                        if (mahLength == 0) {
                            String value = info.substring(keyrowd.length(),info.length());
                            mahLength = value.length();
                        }
                        stringBuilder.append("mAh");
                    } else if (info.startsWith(keyrowd = "POWER_SUPPLY_CHARGE_FULL_DESIGN=")) {
                        stringBuilder.append("设计容量 = ");
                        stringBuilder.append(info.substring(keyrowd.length(), keyrowd.length() + 4));
                        stringBuilder.append("mAh");
                        String value = info.substring(keyrowd.length(),info.length());
                        mahLength = value.length();
                    } else if (info.startsWith(keyrowd = "POWER_SUPPLY_TEMP=")) {
                        stringBuilder.append("电池温度 = ");
                        stringBuilder.append(info.substring(keyrowd.length(), keyrowd.length() + 2));
                        stringBuilder.append("°C");
                    } else if (info.startsWith(keyrowd = "POWER_SUPPLY_TEMP_WARM=")) {
                        stringBuilder.append("警戒温度 = ");
                        int value = Integer.parseInt(info.substring(keyrowd.length(), info.length()));
                        stringBuilder.append(value / 10);
                        stringBuilder.append("°C");
                    } else if (info.startsWith(keyrowd = "POWER_SUPPLY_TEMP_COOL=")) {
                        stringBuilder.append("低温温度 = ");
                        int value = Integer.parseInt(info.substring(keyrowd.length(), info.length()));
                        stringBuilder.append(value / 10);
                        stringBuilder.append("°C");
                    } else if (info.startsWith(keyrowd = "POWER_SUPPLY_VOLTAGE_NOW=")) {
                        stringBuilder.append("当前电压 = ");
                        int v = Integer.parseInt(info.substring(keyrowd.length(), keyrowd.length() + 2));
                        stringBuilder.append(v / 10.0f);
                        stringBuilder.append("v");
                    } else if (info.startsWith(keyrowd = "POWER_SUPPLY_VOLTAGE_MAX_DESIGN=")) {
                        stringBuilder.append("设计电压 = ");
                        int v = Integer.parseInt(info.substring(keyrowd.length(), keyrowd.length() + 2));
                        stringBuilder.append(v / 10.0f);
                        stringBuilder.append("v");
                    } else if (info.startsWith(keyrowd = "POWER_SUPPLY_BATTERY_TYPE=")) {
                        stringBuilder.append("电池类型 = ");
                        stringBuilder.append(info.substring(keyrowd.length(), info.length()));
                    } else if (info.startsWith(keyrowd = "POWER_SUPPLY_CYCLE_COUNT=")) {
                        stringBuilder.append("循环次数 = ");
                        stringBuilder.append(info.substring(keyrowd.length(), info.length()));
                    } /*else if (info.startsWith(keyrowd = "POWER_SUPPLY_TIME_TO_EMPTY_AVG=")) {
                        stringBuilder.append("平均耗尽 = ");
                        int val = Integer.parseInt(info.substring(keyrowd.length(), info.length()));
                        stringBuilder.append(((val / 3600.0) + "    ").substring(0, 4));
                        stringBuilder.append("小时");
                    } else if (info.startsWith(keyrowd = "POWER_SUPPLY_TIME_TO_FULL_AVG=")) {
                        stringBuilder.append("平均充满 = ");
                        int val = Integer.parseInt(info.substring(keyrowd.length(), info.length()));
                        stringBuilder.append(((val / 3600.0) + "    ").substring(0, 4));
                        stringBuilder.append("小时");
                    }*/ else if (info.startsWith(keyrowd = "POWER_SUPPLY_CONSTANT_CHARGE_VOLTAGE=")) {
                        stringBuilder.append("充电电压 = ");
                        int v = Integer.parseInt(info.substring(keyrowd.length(), keyrowd.length() + 2));
                        stringBuilder.append(v / 10.0f);
                        stringBuilder.append("v");
                    } else if (info.startsWith(keyrowd = "POWER_SUPPLY_CAPACITY=")) {
                        stringBuilder.append("电池电量 = ");
                        stringBuilder.append(info.substring(keyrowd.length(), ((keyrowd.length() + 2) > info.length()) ? info.length() : (keyrowd.length() + 2)));
                        stringBuilder.append("%");
                    } else if (info.startsWith(keyrowd = "POWER_SUPPLY_CURRENT_NOW=")) {
                        io = info.substring(keyrowd.length(), info.length());
                        continue;
                    } else {
                        continue;
                    }
                    stringBuilder.append("\n");
                } catch (Exception ignored) {
                }
            }

            if (io.length() > 0 && mahLength != 0) {
                int val = (mahLength < 5) ? (Integer.parseInt(io)):((int) (Integer.parseInt(io) / Math.pow(10, mahLength - 4)));
                stringBuilder.insert(0, "放电速度 = "+ val +"mA\n");
            }

            return stringBuilder.toString();
        } else {
            return "";
        }
    }

    //获取电池容量
    public String getBatteryMAH() {
        String path = "";
        if (new File("/sys/class/power_supply/bms/uevent").exists()) {
            /*
            POWER_SUPPLY_NAME=bms
            POWER_SUPPLY_CAPACITY=30
            POWER_SUPPLY_CAPACITY_RAW=77
            POWER_SUPPLY_TEMP=320
            POWER_SUPPLY_VOLTAGE_NOW=3697500
            POWER_SUPPLY_VOLTAGE_OCV=3777837
            POWER_SUPPLY_CURRENT_NOW=440917
            POWER_SUPPLY_RESISTANCE_ID=58000
            POWER_SUPPLY_RESISTANCE=200195
            POWER_SUPPLY_BATTERY_TYPE=sagit_atl
            POWER_SUPPLY_CHARGE_FULL_DESIGN=3349000
            POWER_SUPPLY_VOLTAGE_MAX_DESIGN=4400000
            POWER_SUPPLY_CYCLE_COUNT=69
            POWER_SUPPLY_CYCLE_COUNT_ID=1
            POWER_SUPPLY_CHARGE_NOW_RAW=1244723
            POWER_SUPPLY_CHARGE_NOW=0
            POWER_SUPPLY_CHARGE_FULL=3272000
            POWER_SUPPLY_CHARGE_COUNTER=1036650
            POWER_SUPPLY_TIME_TO_FULL_AVG=26438
            POWER_SUPPLY_TIME_TO_EMPTY_AVG=30561
            POWER_SUPPLY_SOC_REPORTING_READY=1
            POWER_SUPPLY_DEBUG_BATTERY=0
            POWER_SUPPLY_CONSTANT_CHARGE_VOLTAGE=4389899
            POWER_SUPPLY_CC_STEP=0
            POWER_SUPPLY_CC_STEP_SEL=0
            */
            String batteryInfos = KernelProrp.getProp("/sys/class/power_supply/bms/uevent");
            if (batteryInfos != null) {
                String[] arr = batteryInfos.split("\n");
                String[] keywords = new String[]{"POWER_SUPPLY_CHARGE_FULL=", "POWER_SUPPLY_CHARGE_FULL_DESIGN="};
                for (int k = 0; k < keywords.length; k++) {
                    String keyword = keywords[k];
                    for (int i = 0; i < arr.length; i++) {
                        if (arr[i].startsWith(keyword)) {
                            String chargeFull = arr[i];
                            chargeFull = chargeFull.substring(keyword.length());
                            if (chargeFull.length() > 4)
                                chargeFull = chargeFull.substring(0, 4);
                            return chargeFull + "mAh";
                        }
                    }
                }
            }

        } else {
            if (new File("/sys/class/power_supply/battery/charge_full").exists()) {
                path = "/sys/class/power_supply/battery/charge_full";
            } else if (new File("/sys/class/power_supply/battery/charge_full_design").exists()) {
                path = "/sys/class/power_supply/battery/charge_full_design";
            } else if (new File("/sys/class/power_supply/battery/full_bat").exists()) {
                path = "/sys/class/power_supply/battery/full_bat";
            } else {
                return "? mAh";
            }
            String txt = KernelProrp.getProp(path);
            if (txt == null || txt.trim().length() == 0)
                return "? mAh";
            if (txt.length() > 4)
                return txt.substring(0, 4) + " mAh";
            return txt + " mAh";
        }
        return "? mAh";
    }

    //获取充电速度
    public String getChangeMAH() {
        // /sys/class/power_supply/battery/current_now
        String path = "";
        if (new File("/sys/class/power_supply/battery/current_now").exists()) {
            path = "/sys/class/power_supply/battery/current_now";
        } else if (new File("/sys/class/power_supply/battery/BatteryAverageCurrent").exists()) {
            path = "/sys/class/power_supply/battery/BatteryAverageCurrent";
        } else {
            return "? mA";
        }
        String txt = KernelProrp.getProp(path);

        try {
            int ma = Math.abs(Integer.parseInt(txt));
            if (ma < 1000)
                return ma + " mA";
            return (ma / 1000) + " mA";
        } catch (Exception ex) {
            return "? mA";
        }
    }

    //修改充电速度限制
    public void setChargeInputLimit(int limit) {
        String cmd =
                "echo 0 > /sys/class/power_supply/battery/restricted_charging;" +
                "echo 0 > /sys/class/power_supply/battery/safety_timer_enabled;" +
                "chmod 644 /sys/class/power_supply/bms/temp_warm;" +
                "echo 480 > /sys/class/power_supply/bms/temp_warm;" +
                "chmod 644 /sys/class/power_supply/battery/constant_charge_current_max;" +
                "echo 2000000 > /sys/class/power_supply/battery/constant_charge_current_max;" +
                "echo 2500000 > /sys/class/power_supply/battery/constant_charge_current_max;" +
                "echo 3000000 > /sys/class/power_supply/battery/constant_charge_current_max;" +
                "echo 3500000 > /sys/class/power_supply/battery/constant_charge_current_max;" +
                "echo 4000000 > /sys/class/power_supply/battery/constant_charge_current_max;" +
                "echo 4500000 > /sys/class/power_supply/battery/constant_charge_current_max;" +
                "echo 5000000 > /sys/class/power_supply/battery/constant_charge_current_max;" +
                "echo " + limit + "000 > /sys/class/power_supply/battery/constant_charge_current_max;";

        new SuDo(null).execCmdSync(cmd);
    }
}
