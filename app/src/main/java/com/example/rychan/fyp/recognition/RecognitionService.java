package com.example.rychan.fyp.recognition;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

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


public class RecognitionService extends IntentService {

    private static final String ARG_RECEIPT_PATH = "receipt_path";
    private static final String ARG_RECEIPT_ID = "receipt_id";

    public RecognitionService() {
        super("RecognitionService");
    }

    public static void startRecognition(Context context, String receiptPath, int receiptId) {
        Intent intent = new Intent(context, RecognitionService.class);
        intent.putExtra(ARG_RECEIPT_PATH, receiptPath);
        intent.putExtra(ARG_RECEIPT_ID, receiptId);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String receiptPath = intent.getStringExtra(ARG_RECEIPT_PATH);
            int receiptId = intent.getIntExtra(ARG_RECEIPT_ID, -1);
            Mat src = Imgcodecs.imread(receiptPath, 0);

            List<Range> segmentationResult = segmentation(src, 0);
            RecognitionResult recognitionResult = textRecognition(src, segmentationResult);
            recognitionResult.save(getContentResolver(), receiptId);
        }
    }


    private List<Range> segmentation1(Mat srcBinary) {

        int width = srcBinary.cols();
        int height = srcBinary.rows();

        Mat rowSum = new Mat();
        Core.reduce(srcBinary, rowSum, 1, Core.REDUCE_SUM, CvType.CV_32S);
        rowSum.convertTo(rowSum, CvType.CV_8U, 1.0/width);

        Mat rowMask = new Mat();
        Imgproc.threshold(rowSum, rowMask, 250, 255, Imgproc.THRESH_BINARY_INV);

        Mat kernel = new Mat(3, 1, CvType.CV_8UC1, Scalar.all(255));
        Imgproc.morphologyEx(rowMask, rowMask, Imgproc.MORPH_OPEN, kernel);

        Mat marker = Mat.zeros(rowMask.rows(), rowMask.cols(), CvType.CV_32SC1);
        int count = 0;
        boolean b = rowMask.get(0, 0)[0] == 255;
        int start = 0;
        for (int y = 0; y < height; ++y) {
            if (!b && rowMask.get(y, 0)[0] == 255) {
                start = y;
                b = true;
            }
            if (b && rowMask.get(y, 0)[0] == 0) {
                marker.submat(start, y-1, 0, 0).setTo(new Scalar(++count));
                b = false;
            }
        }

        Mat rowSumBGR = new Mat();
        Imgproc.cvtColor(rowSum, rowSumBGR, Imgproc.COLOR_GRAY2BGR);
        Imgproc.watershed(rowSumBGR, marker);

        List<Range> rowRangeList = new ArrayList<>();
        start = 0;
        count = 0;
        for (int y = 0; y < height; ++y) {
            if (marker.get(y, 0)[0] != -1) {
                start = y;
            } else {
                ++count;
                rowRangeList.add(new Range(start, y));
            }
        }
        rowRangeList.add(new Range(start, height - 1));
        Collections.reverse(rowRangeList);

        return rowRangeList;
    }

    private List<Range> segmentation(Mat srcBinary, int mode) {

        int width = srcBinary.cols();
        int height = srcBinary.rows();

        Mat rowSum = new Mat();
        Core.reduce(srcBinary, rowSum, 1, Core.REDUCE_SUM, CvType.CV_32S);
        rowSum.convertTo(rowSum, CvType.CV_8U, 1.0/width);

        double threshold = 250;
        switch (mode) {
            case 0:
                Mat sortedSum = new Mat();
                Core.sort(rowSum, sortedSum, Core.SORT_ASCENDING | Core.SORT_EVERY_COLUMN);
                if ((height & 1) == 1) {
                    threshold = sortedSum.get((height - 1) / 2, 0)[0];
                } else {
                    threshold = (sortedSum.get((height) / 2, 0)[0] + sortedSum.get((height) / 2 - 1, 0)[0]) / 2;
                }
                break;
            case 1:
                Mat mean = new Mat();
                Core.reduce(rowSum, mean, 0, Core.REDUCE_AVG);
                break;

            default:
        }

        Mat rowMask = new Mat();
        Imgproc.threshold(rowSum, rowMask, threshold, 255, Imgproc.THRESH_BINARY_INV);

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

    private List<Range> segmentation2(Mat srcBGR) {

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

    private RecognitionResult textRecognition(Mat src, List<Range> segmentationResult){
        RecognitionResult recognitionResult = new RecognitionResult(
                getContentResolver().query(ReceiptProvider.FORMAT_CONTENT_URI,
                        null, null, null, null, null));
        boolean isChi = false;

        TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
        baseApi.init(Environment.getExternalStorageDirectory().getPath(), "eng");
        baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, getResources().getString(R.string.eng_whitelist));

        for (Range r: segmentationResult) {
            Bitmap bm = Bitmap.createBitmap(src.cols(), r.size(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(src.rowRange(r), bm);

            baseApi.setImage(bm);
            String result = baseApi.getUTF8Text();
            if (isChi = !recognitionResult.addEngLine(r, result)) {
                break;
            }
        }
        baseApi.clear();
        baseApi.end();

        if (isChi) {
            baseApi = new TessBaseAPI();
            baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
            baseApi.init(Environment.getExternalStorageDirectory().getPath(), "chi_tra");
            baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, getResources().getString(R.string.chi_whitelist));

            for (Range r: segmentationResult) {
                Bitmap bm = Bitmap.createBitmap(src.cols(), r.size(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(src.rowRange(r), bm);

                baseApi.setImage(bm);
                String result = baseApi.getUTF8Text();
                recognitionResult.addChiLine(r, result);
            }
            baseApi.clear();
            baseApi.end();
        }
        return recognitionResult;
    }
}
