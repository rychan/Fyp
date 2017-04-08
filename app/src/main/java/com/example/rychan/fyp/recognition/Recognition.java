package com.example.rychan.fyp.recognition;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.rychan.fyp.R;
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

public class Recognition extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);

        ListView listView = (ListView) findViewById(R.id.list_view);

        Intent intent = getIntent();
        String receiptPath = intent.getStringExtra("receipt_path");
        Mat receiptMat = Imgcodecs.imread(receiptPath, 0);


        List<Range> rowRangeList = segmentation(receiptMat);
        final RecognitionAdapter myAdapter = new RecognitionAdapter(this, textRecognition(receiptMat, rowRangeList), receiptMat);

//
//        List<Range> rowRangeList = new ArrayList<>();
//        rowRangeList.add(new Range(0, receiptMat.rows()));
//        RecognitionAdapter myAdapter = new RecognitionAdapter(this, rowRangeList, receiptMat,
//                textRecognition2(receiptMat));


        listView.setAdapter(myAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override

            public void onItemClick(AdapterView<?> parent, View view, int position, long id){

                RecognitionResult item = myAdapter.getItem(position);
                if (item.type == RecognitionResult.TYPE_NULL) {
                    item.type = RecognitionResult.TYPE_ITEM;
                    myAdapter.notifyDataSetChanged();
                }
            }
        });
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


    private List<RecognitionResult> textRecognition(Mat srcImage, List<Range> rowRangeList){
        List<RecognitionResult> resultList = new ArrayList<>();

        TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.init(Environment.getExternalStorageDirectory().getPath(), "eng");
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);

        for (Range r: rowRangeList) {
            Bitmap bm = Bitmap.createBitmap(srcImage.cols(), r.size(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(srcImage.rowRange(r), bm);

            baseApi.setImage(bm);
            String result = baseApi.getUTF8Text();
            resultList.add(new RecognitionResult(r, result));
        }

        baseApi.clear();
        baseApi.end();
        return resultList;
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
}
