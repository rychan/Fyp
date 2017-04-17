package com.example.rychan.fyp.recognition;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;

import com.example.rychan.fyp.provider.Contract.*;

import org.opencv.core.Range;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rychan on 17年4月5日.
 */
public class RecognitionResult{

    private DateFormat dateFormat;

    private Pattern shopPattern;
    private Pattern datePattern;
    private Pattern totalPattern;
    private Pattern itemPattern;

    private String shopName = null;
    private String date = null;
    private double total = 0;
    private List<LineResult> resultList;

    private boolean shopFound = false;
    private boolean dateFound = false;
    private boolean totalFound = false;


    public RecognitionResult(String shopRegex, String dateFormat, String totalRegex, String itemRegex) {
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

    public void save(ContentResolver contentResolver, int receiptId) {
        ContentValues values = new ContentValues();
        if (shopFound) {
            values.put(ReceiptEntry.COLUMN_SHOP, shopName);
        }
        if (dateFound) {
            values.put(ReceiptEntry.COLUMN_DATE, date);
        }
        if (totalFound) {
            values.put(ReceiptEntry.COLUMN_TOTAL, total);
        } else {
            double temp = 0;
            for (LineResult l : resultList) {
                if (l.type == ItemEntry.TYPE_ITEM) {
                    temp += l.price;
                }
            }
            values.put(ReceiptEntry.COLUMN_TOTAL, temp);
        }

        values.put(ReceiptEntry.COLUMN_STATUS, ReceiptEntry.STATUS_NEW);
        contentResolver.update(
                ContentUris.withAppendedId(ReceiptProvider.RECEIPT_CONTENT_URI, receiptId),
                values, null, null);

        for (LineResult l: resultList) {
            l.save(contentResolver, receiptId);
        }
    }

    public class LineResult {
        String text;
        double price;
        int type;
        int startRow;
        int endRow;

        LineResult(Range r, String s) {
            this.text = s;
            this.price = 0;
            this.startRow = r.start;
            this.endRow = r.end;
            this.type = ItemEntry.TYPE_OTHER;
        }

        public void classify() {
            Matcher shopMatcher = shopPattern.matcher(text.replace("\\s",""));
            Matcher totalMatcher = totalPattern.matcher(text);
            Matcher itemMatcher = itemPattern.matcher(text);
            Matcher dateMatcher = datePattern.matcher(text);

            if (!shopFound && shopMatcher.find()) {
                text = shopMatcher.group(1);
                shopName = text;
                shopFound = true;
            } else if (!totalFound && totalMatcher.find()) {
                text = totalMatcher.group(1);
                price = string2double(totalMatcher.group(2).replace("\\s",""));
                total = price;
                totalFound = true;
            } else if (!totalFound && itemMatcher.find()) {
                text = itemMatcher.group(1);
                price = string2double(itemMatcher.group(2).replace("\\s",""));
                type = ItemEntry.TYPE_ITEM;
            } else if (!dateFound && dateMatcher.find()) {
                try {
                    DateFormat databaseFormat = new SimpleDateFormat("yyyy-MM-dd");
                    text = dateMatcher.group(1);
                    date = databaseFormat.format(dateFormat.parse(text));
                    dateFound = true;
                } catch (ParseException e) {
                }
            }
        }

        private double string2double(String s) {
            try {
                return Double.valueOf(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public void save(ContentResolver contentResolver, int receiptId) {
            ContentValues values = new ContentValues();
            values.put(ItemEntry.COLUMN_NAME, text);
            values.put(ItemEntry.COLUMN_PRICE, price);
            values.put(ItemEntry.COLUMN_RECEIPT_ID, receiptId);
            values.put(ItemEntry.COLUMN_START_ROW, startRow);
            values.put(ItemEntry.COLUMN_END_ROW, endRow);
            values.put(ItemEntry.COLUMN_TYPE, type);
            contentResolver.insert(ReceiptProvider.ITEM_CONTENT_URI, values);
        }
    }
}
