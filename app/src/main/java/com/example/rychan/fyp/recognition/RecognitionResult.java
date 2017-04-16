package com.example.rychan.fyp.recognition;

import org.opencv.core.Mat;
import org.opencv.core.Range;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    public BigDecimal total = new BigDecimal(0);
    public ArrayList<LineResult> resultList;

    private DateFormat dateFormat;

    private Pattern shopPattern;
    private Pattern datePattern;
    private Pattern totalPattern;
    private Pattern itemPattern;

    private boolean shopFound = false;
    private boolean dateFound = false;
    private boolean totalFound = false;

    public RecognitionResult(Mat receiptImage, String shopRegex, String dateFormat, String totalRegex, String itemRegex) {
        this.receiptImage = receiptImage;
        this.shopPattern = Pattern.compile(shopRegex);
        this.dateFormat = new SimpleDateFormat(dateFormat);
        this.datePattern = Pattern.compile(".*(" + dateFormat.replaceAll("\\w","\\d") + ").*");
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
            if (l.type == TYPE_ITEM) {
                totalPrice = totalPrice.add(l.number);
            }
        }
        total = totalPrice;
        setChanged();
        notifyObservers();
    }

    public class LineResult {
        Range rowRange;
        String text;
        BigDecimal number;
        int type = TYPE_NULL;

        LineResult(Range r, String s) {
            this.rowRange = r;
            this.text = s;
            this.number = new BigDecimal(0);
        }

        void classify() {
            Matcher shopMatcher = shopPattern.matcher(text.replace("\\s",""));
            Matcher totalMatcher = totalPattern.matcher(text);
            Matcher itemMatcher = itemPattern.matcher(text);
            Matcher dateMatcher = datePattern.matcher(text);

            if (shopMatcher.find() && !shopFound) {
                type = TYPE_SHOP;
                text = shopMatcher.group(1);
                shopName = text;
                shopFound = true;
            } else if (totalMatcher.find() && !totalFound) {
                type = TYPE_TOTAL;
                text = totalMatcher.group(1);
                number = string2number(totalMatcher.group(2).replace("\\s",""));
                total = number;
                totalFound = true;
            } else if (itemMatcher.find() && !totalFound) {
                type = TYPE_ITEM;
                text = itemMatcher.group(1);
                number = string2number(itemMatcher.group(2).replace("\\s",""));
            } else if (dateMatcher.find() && !dateFound) {
                try {
                    DateFormat databaseFormat = new SimpleDateFormat("yyyy-MM-dd");

                    type = TYPE_DATE;
                    text = dateMatcher.group(1);
                    date = databaseFormat.format(dateFormat.parse(text));
                    dateFound = true;
                } catch (ParseException e) {
                    type = TYPE_NULL;
                }
            } else {
                type = TYPE_NULL;
            }
        }

        private BigDecimal string2number(String s) {
            if (s == null || s.isEmpty()) {
                return new BigDecimal(0);
            } else {
                return new BigDecimal(s);
            }
        }
    }
}
