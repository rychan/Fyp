package com.example.rychan.fyp.recognition;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.example.rychan.fyp.DatePickerDialogFragment;
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

    private DateFormat dateFormat = DatePickerDialogFragment.dateFormat;
    private Pattern datePattern = Pattern.compile(".*(\\d{4}-\\d{2}-\\d{2}).*");;
    private Pattern totalPattern = Pattern.compile(".*(Total|小計)\\D*(\\d*\\s*\\.\\s*\\d{1,2}).*");
    private Pattern itemPattern = Pattern.compile("(.*)\\s+\\D*(\\d*\\s*\\.\\s*\\d{1,2}).*");

    private String shop = null;
    private String date = null;
    private double total = 0;
    private List<LineResult> resultList = new ArrayList<>();

    private boolean shopFound = false;
    private boolean dateFound = false;
    private boolean totalFound = false;

    private Cursor cursor;


    public RecognitionResult(Cursor cursor) {
        this.cursor = cursor;
    }

    public boolean addEngLine(Range r, String s) {
        LineResult lineResult = new LineResult(r, s);
        if (shopFound) {
            lineResult.classify();
            resultList.add(lineResult);
            return true;

        } else if (shopFound = lineResult.findShop()) {

            String dateString = cursor.getString(cursor.getColumnIndex(FormatEntry.COLUMN_DATE_FORMAT));
            dateFormat = new SimpleDateFormat(dateString);
            datePattern = Pattern.compile(".*(" + dateString.replaceAll("\\w", "\\\\d") + ").*");
            totalPattern = Pattern.compile(cursor.getString(cursor.getColumnIndex(FormatEntry.COLUMN_TOTAL_FORMAT)));
            itemPattern = Pattern.compile(cursor.getString(cursor.getColumnIndex(FormatEntry.COLUMN_ITEM_FORMAT )));

            if (cursor.getString(cursor.getColumnIndex(FormatEntry.COLUMN_LANG)).equals("eng")) {
                resultList.add(lineResult);
                for (LineResult l : resultList) {
                    l.classify();
                }
                return true;

            } else {
                resultList.clear();
                return false;
            }
        } else {
            resultList.add(lineResult);
            return true;
        }
    }

    public void addChiLine(Range r, String s) {
        LineResult lineResult = new LineResult(r, s);
        lineResult.classify();
        resultList.add(lineResult);
    }

    public void save(ContentResolver contentResolver, int receiptId) {
        ContentValues values = new ContentValues();
        if (shopFound) {
            values.put(ReceiptEntry.COLUMN_SHOP, shop);
        } else {
            for (LineResult l : resultList) {
                l.classify();
            }
        }
        if (dateFound) {
            values.put(ReceiptEntry.COLUMN_DATE, date);
        }
        if (!totalFound) {
            total = 0;
            for (LineResult l : resultList) {
                if (l.type == ItemEntry.TYPE_ITEM) {
                    total += l.price;
                }
            }
        }
        values.put(ReceiptEntry.COLUMN_TOTAL, total);

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
        double price = 0;
        int type = ItemEntry.TYPE_OTHER;
        int startRow;
        int endRow;

        LineResult(Range r, String s) {
            this.text = s;
            this.startRow = r.start;
            this.endRow = r.end;
        }

        public boolean findShop() {
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                String noSpace = text.replaceAll("\\s+","");
                String s = cursor.getString(cursor.getColumnIndex(FormatEntry.COLUMN_SHOP));
                if (noSpace.contains(s)) {
                    shop = s;
                    return true;
                }
            }
            return false;
        }

        public void classify() {
            Matcher totalMatcher = totalPattern.matcher(text);
            Matcher itemMatcher = itemPattern.matcher(text);
            Matcher dateMatcher = datePattern.matcher(text);

            if (!totalFound && totalMatcher.find()) {
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
                    DateFormat databaseFormat = DatePickerDialogFragment.dateFormat;
                    text = dateMatcher.group(1);
                    date = databaseFormat.format(dateFormat.parse(text));
                    dateFound = true;
                } catch (ParseException e) {
                    Log.e("Recognition", "Cannot parse date");
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
