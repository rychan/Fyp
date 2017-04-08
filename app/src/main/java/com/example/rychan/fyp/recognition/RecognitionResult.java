package com.example.rychan.fyp.recognition;

import org.opencv.core.Range;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rychan on 17年4月5日.
 */

public class RecognitionResult {
    public static final int TYPE_NULL = 0;
    public static final int TYPE_ITEM = 1;
    public static final int TYPE_TOTAL = 2;

    Range rowRange;
    String result1;
    String result2;
    int type;

    RecognitionResult(Range r, String s){
        rowRange = r;

        String regex = "(.*)(\\$)(\\d*\\.\\d)";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(s);

        if (m.find()) {
            type = TYPE_ITEM;
            result1 = m.group(1);
            result2 = m.group(3);
        } else {
            type = TYPE_NULL;
            result1 = s;
            result2 = null;
        }
    }
}
