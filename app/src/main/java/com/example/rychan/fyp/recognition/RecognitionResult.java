package com.example.rychan.fyp.recognition;

import org.opencv.core.Mat;
import org.opencv.core.Range;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Observable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rychan on 17年4月5日.
 */
public class RecognitionResult extends Observable {
    public static final int TYPE_NULL = 0;
    public static final int TYPE_ITEM = 1;
    public static final int TYPE_SHOP = 2;
    public static final int TYPE_DATE = 3;
    public static final int TYPE_TOTAL = 4;

    public Mat receiptImage;

    public String shopName = null;
    public String date = null;
    public String total = null;
    public ArrayList<LineResult> resultList;

    private Pattern shopPattern;
    private Pattern datePattern;
    private Pattern totalPattern;
    private Pattern itemPattern;

    private boolean shopFound = false;
    private boolean dateFound = false;
    private boolean totalFound = false;

    public RecognitionResult(Mat receiptImage, String shopRegex, String dateRegex, String totalRegex, String itemRegex) {
        this.receiptImage = receiptImage;
        this.shopPattern = Pattern.compile(shopRegex);
        this.datePattern = Pattern.compile(dateRegex);
        this.totalPattern = Pattern.compile(totalRegex);
        this.itemPattern = Pattern.compile(itemRegex);
        this.resultList = new ArrayList<>();
    }

    public void addLine(Range r, String s) {
        LineResult lineResult = new LineResult(r, s);
        lineResult.classify();
        resultList.add(lineResult);
    }

    public void computeTotal(){
        BigDecimal totalPrice = new BigDecimal(0);
        for (LineResult l: resultList) {
            if (l.type == TYPE_ITEM && l.result2 != null && !l.result2.isEmpty()) {
                totalPrice = totalPrice.add(new BigDecimal(l.result2));
            }
        }
        total = totalPrice.toString();
        setChanged();
        notifyObservers();
    }

    public class LineResult {
        Range rowRange;
        String result1;
        String result2 = null;
        int type = TYPE_NULL;;

        LineResult(Range r, String s) {
            this.rowRange = r;
            this.result1 = s;
        }

        void classify() {
            Matcher itemMatcher = itemPattern.matcher(result1);
            Matcher totalMatcher = totalPattern.matcher(result1);
            Matcher dateMatcher = datePattern.matcher(result1);
            Matcher shopMatcher = shopPattern.matcher(result1);

            if (shopMatcher.find() && !shopFound) {
                type = TYPE_SHOP;
                result1 = shopMatcher.group(1);
                shopName = result1;
                shopFound = true;
            } else if (dateMatcher.find() && !dateFound) {
                type = TYPE_DATE;
                result1 = dateMatcher.group(1);
                date = result1;
                dateFound = true;
            } else if (totalMatcher.find() && !totalFound) {
                type = TYPE_TOTAL;
                result1 = totalMatcher.group(1);
                result2 = totalMatcher.group(2);
                total = result2;
                totalFound = true;
            } else if (itemMatcher.find() && !totalFound) {
                type = TYPE_ITEM;
                result1 = itemMatcher.group(1);
                result2 = itemMatcher.group(2);
            } else {
                type = TYPE_NULL;
            }
        }
    }
}
