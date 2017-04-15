package com.example.rychan.fyp.recognition;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.rychan.fyp.R;
import com.example.rychan.fyp.provider.Contract.*;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class RecognitionActivity extends AppCompatActivity implements Observer, View.OnClickListener {
    private RecognitionResult recognitionResult;
    String receiptPath;
    private TextView totalText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);

        ListView listView = (ListView) findViewById(R.id.list_view);

        Intent intent = getIntent();
        receiptPath = intent.getStringExtra("receipt_path");
        Mat receiptMat = Imgcodecs.imread(receiptPath, 0);


        List<Range> rowRangeList = segmentation(receiptMat);
        recognitionResult = textRecognition(receiptMat, rowRangeList);
        recognitionResult.addObserver(this);
        final RecognitionAdapter myAdapter = new RecognitionAdapter(this, recognitionResult);

//
//        List<Range> rowRangeList = new ArrayList<>();
//        rowRangeList.add(new Range(0, receiptMat.rows()));
//        RecognitionAdapter myAdapter = new RecognitionAdapter(this, rowRangeList, receiptMat,
//                textRecognition2(receiptMat));

        EditText shopNameText = (EditText) findViewById(R.id.shopName);
        shopNameText.setText(recognitionResult.shopName);

        EditText dateText = (EditText) findViewById(R.id.date);
        dateText.setText(recognitionResult.date);

        totalText = (TextView) findViewById(R.id.total);
        totalText.setText(recognitionResult.total);

        listView.setAdapter(myAdapter);

        Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(this);
    }

    private List<Range> segmentation(Mat srcBinary) {

        int width = srcBinary.cols();
        int height = srcBinary.rows();

        Mat rowSum = new Mat();
        Core.reduce(srcBinary, rowSum, 1, Core.REDUCE_SUM, CvType.CV_32S);
        rowSum.convertTo(rowSum, CvType.CV_8U, 1.0/width);

        Mat rowMask = new Mat();
        Imgproc.threshold(rowSum, rowMask, 250, 255, Imgproc.THRESH_BINARY_INV);

        Mat kernel = new Mat(5, 1, CvType.CV_8UC1, Scalar.all(255));
        Imgproc.morphologyEx(rowMask, rowMask, Imgproc.MORPH_DILATE, kernel);

        Mat mask = new Mat(height, width, CvType.CV_8UC1);
        for (int i = 0; i < width; ++i) {
            rowMask.copyTo(mask.col(i));
        }

        List<MatOfPoint> contour = new ArrayList<>();
        Imgproc.findContours(mask, contour, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        List<Range> rowRangeList = new ArrayList<>();
        for (MatOfPoint c: contour) {
            Rect rect = Imgproc.boundingRect(c);
            rowRangeList.add(new Range(rect.y, rect.y + rect.height));
        }
        Collections.reverse(rowRangeList);

        return rowRangeList;
    }


    private List<Range> segmentation1(Mat srcBGR) {

        int width = srcBGR.cols();
        int height = srcBGR.rows();

        Mat gray = new Mat();
        Imgproc.cvtColor(srcBGR, gray, Imgproc.COLOR_BGR2GRAY);

        // get edges of the image
        double highThreshold = Imgproc.threshold(gray, new Mat(), 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        double lowThreshold = highThreshold * 0.5;
        Mat canny = new Mat();
        Imgproc.Canny(gray, canny, lowThreshold, highThreshold);

        Mat rowSum = new Mat();
        Core.reduce(canny, rowSum, 1, Core.REDUCE_SUM, CvType.CV_32S);
        rowSum.convertTo(rowSum, CvType.CV_8U, 1.0/width);

        Mat rowMask = new Mat();
        Imgproc.threshold(rowSum, rowMask, 2, 255, Imgproc.THRESH_BINARY);

        Mat kernel = new Mat(15, 1, CvType.CV_8UC1, Scalar.all(255));
        Imgproc.morphologyEx(rowMask, rowMask, Imgproc.MORPH_DILATE, kernel);

        Mat mask = new Mat(height, width, CvType.CV_8UC1);
        for (int i = 0; i < width; ++i) {
            rowMask.copyTo(mask.col(i));
        }

        List<MatOfPoint> contour = new ArrayList<>();
        Imgproc.findContours(mask, contour, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        List<Range> rowRangeList = new ArrayList<>();
        for (MatOfPoint c: contour) {
            Rect rect = Imgproc.boundingRect(c);
            rowRangeList.add(new Range(rect.y, rect.y + rect.height));
        }
        Collections.reverse(rowRangeList);

        return rowRangeList;
    }


    private RecognitionResult textRecognition(Mat srcImage, List<Range> rowRangeList){
        RecognitionResult recognitionResult = new RecognitionResult(
                srcImage,
                ".*(STARBUCKS).*",
                ".*(\\d{4}/\\d{2}/\\d{2}).*",
                ".*(Total).*\\$(\\d*\\s*\\.\\s*\\d)",
                "(.*)\\$(\\d*\\s*\\.\\s*\\d)"
        );
        String whitelist = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890$/.";

        TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
        baseApi.init(Environment.getExternalStorageDirectory().getPath(), "eng");
        baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, whitelist);

        for (Range r: rowRangeList) {
            Bitmap bm = Bitmap.createBitmap(srcImage.cols(), r.size(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(srcImage.rowRange(r), bm);

            baseApi.setImage(bm);
            String result = baseApi.getUTF8Text();
            recognitionResult.addLine(r, result);
        }
        baseApi.clear();
        baseApi.end();
        return recognitionResult;
    }

    private List<String> textRecognition2(Mat srcImage){
        List<String> resultList = new ArrayList<>();

        TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.init(Environment.getExternalStorageDirectory().getPath(), "eng");
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT);

        Bitmap bm = Bitmap.createBitmap(srcImage.cols(), srcImage.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(srcImage, bm);

        baseApi.setImage(bm);
        resultList.add(baseApi.getUTF8Text());


        baseApi.clear();
        baseApi.end();
        return resultList;
    }

    @Override
    public void update(Observable o, Object arg) {
        totalText.setText(recognitionResult.total);
    }

    @Override
    public void onClick(View v) {
        ContentValues value = new ContentValues();
        if (recognitionResult.shopName == null || recognitionResult.shopName.isEmpty()) {
            value.put(ReceiptEntry.COLUMN_SHOP, "unknown");
        } else {
            value.put(ReceiptEntry.COLUMN_SHOP, recognitionResult.shopName);
        }
        if (recognitionResult.date == null || recognitionResult.date.isEmpty()) {
            value.put(ReceiptEntry.COLUMN_DATE, "unknown");
        } else {
            value.put(ReceiptEntry.COLUMN_DATE, recognitionResult.date);
        }
        value.put(ReceiptEntry.COLUMN_TOTAL, Double.valueOf(recognitionResult.total));
        value.put(ReceiptEntry.COLUMN_FILE, receiptPath);
        Uri uri = getContentResolver().insert(ReceiptProvider.RECEIPT_CONTENT_URI, value);
        int id = Integer.valueOf(uri.getLastPathSegment());

        for (RecognitionResult.LineResult l: recognitionResult.resultList) {
            if (l.type == RecognitionResult.TYPE_ITEM && l.result2 != null && !l.result2.isEmpty()) {
                ContentValues itemValue = new ContentValues();
                if (l.result1 == null || l.result1.isEmpty()) {
                    itemValue.put(ItemEntry.COLUMN_ITEM, "unknown");
                } else {
                    itemValue.put(ItemEntry.COLUMN_ITEM, l.result1);
                }
                if (l.result2 == null || l.result2.isEmpty()) {
                    itemValue.put(ItemEntry.COLUMN_PRICE, 0);
                } else {
                    itemValue.put(ItemEntry.COLUMN_PRICE, Double.valueOf(l.result2));
                }
                itemValue.put(ItemEntry.COLUMN_RECEIPT_ID, id);
                getContentResolver().insert(ReceiptProvider.ITEM_CONTENT_URI, itemValue);
            }
        }

        Intent data = new Intent();
        setResult(RESULT_OK, data);
        finish();
    }
}
