package com.omarea.scripts.action;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionParamInfo {
    public String name;
    public String desc;
    public String value;
    public String valueShell;
    public String valueSUShell;
    public String valueFromShell;
    public int maxLength = -1;
    public String type;
    public boolean readonly;
    public ArrayList<ActionParamOption> options;
    //FIXME: 这是新增的，后面要把获取options的脚本放到这里，在执行时获取选项，而不是在读取配置时直接获取options
    public String optionsSh = "";
    public String optionsSU = "";

    public static class ParamInfoFilter implements InputFilter {
        private ActionParamInfo paramInfo;

        public ParamInfoFilter(ActionParamInfo paramInfo) {
            this.paramInfo = paramInfo;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (source != null && source.toString().contains("\"")) {
                return "";
            }

            if (paramInfo.maxLength >= 0) {
                int keep = paramInfo.maxLength - (dest.length() - (dend - dstart));
                if (keep <= 0) {
                    // 如果超出字数限制，就返回“”
                    return "";
                }
            }

            if (paramInfo.type != null && !paramInfo.type.equals("") && source != null) {
                if (paramInfo.type.equals("int")) {
                    Pattern regex = Pattern.compile("^[0-9]{0,}$");
                    Matcher matcher = regex.matcher(source.toString());
                    if (!matcher.matches()) {
                        return "";
                    }
                } else if (paramInfo.type.equals("number")) {
                    Pattern regex = Pattern.compile("^[\\-.,0-9]{0,}$");
                    Matcher matcher = regex.matcher(source.toString());
                    if (!matcher.matches()) {
                        return "";
                    }
                }
            }
            return null;
        }
    }

    public static class ActionParamOption {
        public String value;
        public String desc;
    }
}
