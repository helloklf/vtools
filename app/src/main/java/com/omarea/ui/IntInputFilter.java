package com.omarea.ui;

import android.text.InputFilter;
import android.text.Spanned;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntInputFilter implements InputFilter {
    private int maxLength = 3;
    private String type = "int";

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        if (source != null && source.toString().contains("\"")) {
            return "";
        }

        if (maxLength >= 0) {
            int keep = maxLength - (dest.length() - (dend - dstart));
            if (keep <= 0) {
                // 如果超出字数限制，就返回“”
                return "";
            }
        }

        if (type != null && !type.equals("") && source != null) {
            if (type.equals("int")) {
                Pattern regex = Pattern.compile("^[0-9]{0,}$");
                Matcher matcher = regex.matcher(source.toString());
                if (!matcher.matches()) {
                    return "";
                }
            } else if (type.equals("number")) {
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