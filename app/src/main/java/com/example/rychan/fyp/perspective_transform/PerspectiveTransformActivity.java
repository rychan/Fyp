package com.example.rychan.fyp.perspective_transform;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;

import com.example.rychan.fyp.MainActivity;
import com.example.rychan.fyp.R;
import com.example.rychan.fyp.perspective_transform.binarization.BinarizationSettingDialog;
import com.example.rychan.fyp.provider.Contract.*;
import com.example.rychan.fyp.recognition.RecognitionService;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


public class PerspectiveTransformActivity extends AppCompatActivity implements
        HoughResultFragment.HoughTransform, DisplayImageFragment.ImageProcessor,
        BinarizationSettingDialog.DialogListener, ReceiptNumberDialog.DialogListener,
        StateBar.OnTabChangeListener {

    private String imagePath;

    private StateBar stateBar;

    private int currentDisplay = 0;
    private List<List<Point>> boundaryList;

    private int blurBlockSize;
    private int thresholdBlockSize;
    private double thresholdConstant;

    private int receiptNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perspective_transform);

        Intent intent = getIntent();
        imagePath = intent.getStringExtra("receipt_path");

        stateBar = (StateBar) findViewById(R.id.state_bar);

        getBinarizationSetting();

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            DialogFragment dialog = new ReceiptNumberDialog();
            dialog.show(getSupportFragmentManager(), "ReceiptNumberDialog");
        }
    }

    private void getBinarizationSetting() {
        SharedPreferences binarizationSetting = getSharedPreferences("BINARIZATION_SETTING", Context.MODE_PRIVATE);
        blurBlockSize = binarizationSetting.getInt("BLUR_BLOCK_SIZE", 5);
        thresholdBlockSize = binarizationSetting.getInt("THRESHOLD_BLOCK_SIZE", 101);
        thresholdConstant = Double.valueOf(binarizationSetting.getString("THRESHOLD_CONSTANT", "6.0"));
    }

    @Override
    public void onNumSelected(int num) {
        receiptNum = num;
        stateBar.setTabState(true, receiptNum);

        // Create a new Fragment to be placed in the activity layout
        // Add the fragment to the 'fragment_container' FrameLayout
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, HoughResultFragment.newInstance(imagePath), "HOUGH_FRAGMENT")
                .commit();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ( keyCode == KeyEvent.KEYCODE_MENU ) {

            // perform your desired action here
            DialogFragment dialog = new BinarizationSettingDialog();
            dialog.show(getSupportFragmentManager(), "BinarizationSettingDialog");

            // return 'true' to prevent further propagation of the key event
            return true;
        }

        // let the system handle all other key events
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDialogPositiveClick() {
        getBinarizationSetting();
        Fragment f = getSupportFragmentManager().findFragmentByTag("RESULT_FRAGMENT");
        if (f != null) {
            ((DisplayImageFragment) f).processAndDisplay();
        }
    }

    @Override
    public void onTabChange(int tab) {
        Fragment f = getSupportFragmentManager().findFragmentByTag("HOUGH_FRAGMENT");
        if (f != null && f.isVisible()) {
            ((HoughResultFragment) f).setVisiblility(tab);
        } else {
            f = getSupportFragmentManager().findFragmentByTag("RESULT_FRAGMENT");
            if (f != null && f.isVisible()) {
                currentDisplay = tab - 1;
                ((DisplayImageFragment) f).processAndDisplay();
            }
        }
    }


    class Line {
        Point pt1;
        Point pt2;
        Point midPt;

        Line(Point myPt1, Point myPt2) {
            pt1 = myPt1;
            pt2 = myPt2;
            midPt = new Point((pt1.x + pt2.x) / 2, (pt1.y + pt2.y) / 2);
        }

        Point intersection(Line line) {
            double x1 = pt1.x, y1 = pt1.y;
            double x2 = pt2.x, y2 = pt2.y;
            double x3 = line.pt1.x, y3 = line.pt1.y;
            double x4 = line.pt2.x, y4 = line.pt2.y;
            double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);

            if (d == 0) {
                return new Point(-1, -1);
            } else {
                Point pt = new Point();
                pt.x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
                pt.y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
                return pt;
            }
        }
    }

    class xComparator implements Comparator<Line> {
        @Override
        public int compare(Line l1, Line l2) {
            return (l1.midPt.x < l2.midPt.x) ? -1 : (l1.midPt.x == l2.midPt.x) ? 0 : 1;
        }
    }

    class yComparator implements Comparator<Line> {
        @Override
        public int compare(Line l1, Line l2) {
            return (l1.midPt.y < l2.midPt.y) ? -1 : (l1.midPt.y == l2.midPt.y) ? 0 : 1;
        }
    }

    class AreaComparator implements Comparator<MatOfPoint>{
        @Override
        public int compare(MatOfPoint a, MatOfPoint b){
            return (Imgproc.contourArea(a) < Imgproc.contourArea(b)) ? -1 : (Imgproc.contourArea(a) == Imgproc.contourArea(b)) ? 0 : 1;
        }
    }

    @Override
    public List<Rect> detectReceipt(Mat src) {
//        List<Rect> subMatList = new ArrayList<>();
//        subMatList.add(new Rect(0, 0, src.width(), src.height()));
//        return subMatList;

        return multipleReceiptsDetector(src, receiptNum);
    }

    private List<Rect> multipleReceiptsDetector(Mat img, int numberOfReceipts){
        // Gray scale
        Mat gray = new Mat();
        Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);
        Mat adaptiveGray = new Mat();
        Mat medianImg = new Mat();
        Mat medianBin = new Mat();

        // median blur + adaptive gaussian threshold
        Imgproc.medianBlur(gray, medianImg, 5);
        Imgproc.adaptiveThreshold(medianImg, adaptiveGray, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 35, 0);
        Imgproc.medianBlur(adaptiveGray, medianBin, 5);
        List<MatOfPoint> contours = new ArrayList<>();

        // find all contours
        Imgproc.findContours(medianBin, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        Collections.sort(contours, Collections.reverseOrder(new AreaComparator()));

        // init drewContours to store and prevent overlapping
        List<MatOfPoint2f> drewContours = new ArrayList<>();
//        List<Point[]> drewContours = new ArrayList<>();
        // prevent unreasonable contour like background
        double maxArea = img.width() * img.height() / numberOfReceipts;

        // Counters of DrewContours
        int numberOfDrewContours = 0;

        // Counters of Contours iterated
        int indexOfContours = 0;
        List<Rect> outputList = new ArrayList<>();

        double upperBoundOfWHRatio = 4.0;
        double lowerBoundOfWHRatio = 1/upperBoundOfWHRatio;

        int expandMargin = 10;

        int imgw = img.width();
        int imgh = img.height();

        while (numberOfDrewContours < numberOfReceipts && indexOfContours < contours.size()) {
            MatOfPoint contour = contours.get(indexOfContours);
            MatOfPoint2f contour2F = new MatOfPoint2f();
            contour.convertTo(contour2F, CvType.CV_32F);

            RotatedRect rect = Imgproc.minAreaRect(contour2F);
            Point[] points = new Point[4];
            rect.points(points);
            contour2F.fromArray(points);

            // prevent long rectangle
            double whRatio = rect.size.width/rect.size.height;

            if(Imgproc.contourArea(contour) < maxArea && (whRatio > lowerBoundOfWHRatio && whRatio < upperBoundOfWHRatio)){
                boolean overlapped = false;
                int numberOfDrew = numberOfDrewContours;
                // See if there is Drew Contour
                if (numberOfDrew > 0) {
                    for(int i = 0; i < numberOfDrew; i++){
                        List<Point> contourList = contour2F.toList();
                        for(int j = 0; j < contourList.size(); ++j){
                            MatOfPoint2f a = drewContours.get(i);
                            Point b = contourList.get(j);
                            double insideContour = Imgproc.pointPolygonTest(a, b, false);
                            if (insideContour >= 0) {
                                overlapped = true;
                            }
                        }
                    }
                }
                if(!overlapped){
                    ++numberOfDrewContours;
                    drewContours.add(contour2F);
                    Rect outputRect = Imgproc.boundingRect(contour);
//                    int x0 = outputRect.x - expandMargin;
//                    int x1 = outputRect.x + expandMargin + outputRect.width;
//                    int y0 = outputRect.y - expandMargin;
//                    int y1 = outputRect.y + expandMargin + outputRect.height;
//                    x0 = x0 < 0 ? 0 : x0;
//                    y0 = y0 < 0 ? 0 : x0;
//                    x1 = x1 > imgw ? imgw : x1;
//                    y1 = y1 > imgh ? imgh : y1;

//                    outputList.add(new Rect(x0, y0, x1-x0, y1-y0));
//                    Imgproc.drawContours(img, contours, indexOfContours, new Scalar(0,0,255), 5);
//                    Imgproc.rectangle(img, new Point(x0,y0), new Point(x1,y1), new Scalar(255,0,255), 5);


                    outputList.add(outputRect);
//                    Imgproc.drawContours(img, contours, indexOfContours, new Scalar(0,0,255), 5);
//                    Imgproc.rectangle(img, outputRect.tl(), outputRect.br(), new Scalar(255,0,255), 5);

                    // Mat to List of Point?
            }
        }
        ++indexOfContours;
    }
    return outputList;
}

    @Override
    public List<Point> houghTransform(Mat srcBGR) {

        // resize mat2 to resizeBGR to reduce computation
        Mat resizeBGR = new Mat();
        double width = srcBGR.cols(), height = srcBGR.rows();
        double minWidth = 200;
        double resizeScale = Math.max(0.125, minWidth / width);
        double resizeWidth = width * resizeScale, resizeHeight = height * resizeScale;
        Imgproc.resize(srcBGR, resizeBGR, new Size(resizeWidth, resizeHeight), 0, 0, Imgproc.INTER_AREA);

        // convert to grayscale image
        Mat gray = new Mat();
        Imgproc.cvtColor(resizeBGR, gray, Imgproc.COLOR_BGR2GRAY);

        Mat canny = new Mat();

        // get edges of the image
        double highThreshold = Imgproc.threshold(gray, new Mat(), 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        double lowThreshold = highThreshold * 0.5;
        Imgproc.Canny(gray, canny, lowThreshold, highThreshold);

        Mat lines = new Mat();
        Imgproc.HoughLinesP(canny, lines, 1, Math.PI / 180, (int) resizeWidth / 3, resizeWidth / 3, 20);

        List<Line> horizontals = new ArrayList<>();
        List<Line> verticals = new ArrayList<>();
        for (int i = 0; i < lines.rows(); ++i) {
            double[] v = lines.get(i, 0);
            double deltaX = v[0] - v[2];
            double deltaY = v[1] - v[3];
            Line l = new Line(new Point(v[0], v[1]), new Point(v[2], v[3]));

            // get horizontal lines and vertical lines respectively
            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                horizontals.add(l);
            } else {
                verticals.add(l);
            }
        }

        // handle cases when not enough lines are detected
        if (horizontals.size() < 2) {
            if (horizontals.size() == 0 || horizontals.get(0).midPt.y > resizeHeight / 2) {
                horizontals.add(new Line(new Point(0, 0), new Point(resizeWidth - 1, 0)));
            }
            if (horizontals.size() == 0 || horizontals.get(0).midPt.y <= resizeHeight / 2) {
                horizontals.add(new Line(new Point(0, resizeHeight - 1), new Point(resizeWidth - 1, resizeHeight - 1)));
            }
        }
        if (verticals.size() < 2) {
            if (verticals.size() == 0 || verticals.get(0).midPt.x > resizeWidth / 2) {
                verticals.add(new Line(new Point(0, 0), new Point(0, resizeHeight - 1)));
            }
            if (verticals.size() == 0 || verticals.get(0).midPt.x <= resizeWidth / 2) {
                verticals.add(new Line(new Point(resizeWidth - 1, 0), new Point(resizeWidth - 1, resizeHeight - 1)));
            }
        }

        // sort lines according to their center point
        Collections.sort(horizontals, new yComparator());
        Collections.sort(verticals, new xComparator());

        List<Point> resizePointList = new ArrayList<>(4);
        resizePointList.add(horizontals.get(0).intersection(verticals.get(0)));
        resizePointList.add(horizontals.get(0).intersection(verticals.get(verticals.size() - 1)));
        resizePointList.add(horizontals.get(horizontals.size() - 1).intersection(verticals.get(0)));
        resizePointList.add(horizontals.get(horizontals.size() - 1).intersection(verticals.get(verticals.size() - 1)));

        List<Point> pointList = new ArrayList<>(4);
        for (Point point : resizePointList) {
            pointList.add(new Point((point.x + 0.5) / resizeScale, (point.y + 0.5) / resizeScale));
        }

        return pointList;
    }

    @Override
    public void onBoundaryDetected(List<List<Point>> boundaryList) {
        this.boundaryList = boundaryList;
        stateBar.setTabState(false, receiptNum);
        currentDisplay = 0;

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Create new fragment
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, DisplayImageFragment.newInstance(imagePath),
                "RESULT_FRAGMENT");
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public Mat processImage(Mat src) {
        return pTransform(src, boundaryList.get(currentDisplay));
    }

    @Override
    public void onFinishDisplay(Mat dstMat) {
        File photoFile = null;
        String receiptPath = "";
        try {
            File path = getExternalFilesDir("Receipts");
            path.mkdirs();
            photoFile = MainActivity.createFile("Receipt", ".pbm", path);
            receiptPath = photoFile.getAbsolutePath();
        } catch (IOException ex) {
            // Error occurred while creating the File
            // Log.d(TAG, "Cannot create file");
        }

        // Continue only if the File was successfully created
        if (photoFile != null) {
            Imgcodecs.imwrite(receiptPath, dstMat);

            ContentValues values = new ContentValues();
            values.put(ReceiptEntry.COLUMN_SHOP, "Unknown Shop");
            values.put(ReceiptEntry.COLUMN_DATE, new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            values.put(ReceiptEntry.COLUMN_TOTAL, 0);
            values.put(ReceiptEntry.COLUMN_FILE, receiptPath);
            values.put(ReceiptEntry.COLUMN_STATUS, ReceiptEntry.STATUS_PROCESSING);
            Uri uri = getContentResolver().insert(ReceiptProvider.RECEIPT_CONTENT_URI, values);
            int receiptId = Integer.valueOf(uri.getLastPathSegment());

            RecognitionService.startActionRecognition(this, receiptPath, receiptId);
            stateBar.setUnclickable(currentDisplay + 1);
            ++currentDisplay;
            if (currentDisplay < boundaryList.size()) {
                Fragment f = getSupportFragmentManager().findFragmentByTag("RESULT_FRAGMENT");
                if (f != null) {
                    ((DisplayImageFragment) f).processAndDisplay();
                }
            } else {
                finish();
            }
        }
    }

    private Mat pTransform(Mat src, List<Point> boundary) {

        /* perspective transformation */

        // get points
        Point topLeft = boundary.get(0);
        Point topRight = boundary.get(1);
        Point bottomLeft = boundary.get(2);
        Point bottomRight = boundary.get(3);

        // define the destination image size
        int avgWidth = (int) ((-topLeft.x + topRight.x - bottomLeft.x + bottomRight.x) / 2);
        int avgHeight = (int) ((-topLeft.y - topRight.y + bottomLeft.y + bottomRight.y) / 2);
        Mat transformedMat = Mat.zeros(avgHeight, avgWidth, CvType.CV_8UC3);

        // find corners of destination image with the sequence [topLeft, topRight, bottomLeft, bottomRight]
        Mat dstPoints = new Mat(4, 1, CvType.CV_32FC2);
        dstPoints.put(0, 0,
                0.0, 0.0,
                avgWidth - 1, 0,
                0, avgHeight - 1,
                avgWidth - 1, avgHeight - 1);

        // find corners of source image with the sequence [topLeft, topRight, bottomLeft, bottomRight]
        Mat srcPoints = new Mat(4, 1, CvType.CV_32FC2);
        srcPoints.put(0, 0,
                topLeft.x, topLeft.y,
                topRight.x, topRight.y,
                bottomLeft.x, bottomLeft.y,
                bottomRight.x, bottomRight.y);

        // get transformation matrix
        Mat transMat = Imgproc.getPerspectiveTransform(srcPoints, dstPoints);

        // apply perspective transformation
        Imgproc.warpPerspective(src, transformedMat, transMat, transformedMat.size());

        // eliminate boundary
        int xMargin = 24/receiptNum;
        int yMargin = 24/receiptNum;
        Mat subMat = transformedMat.submat(yMargin, avgHeight - yMargin, xMargin, avgWidth - xMargin);

        // convert to grayscale image
        Mat dst = new Mat();
        Imgproc.cvtColor(subMat, dst, Imgproc.COLOR_BGR2GRAY);

        //Imgproc.threshold(gray, dst, 0, 255, Imgproc.THRESH_OTSU);

        Imgproc.medianBlur(dst, dst, blurBlockSize);
        Imgproc.adaptiveThreshold(dst, dst, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, thresholdBlockSize, thresholdConstant);

        return dst;
    }
}