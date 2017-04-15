package com.example.rychan.fyp.perspective_transform;

import android.content.Intent;
import android.graphics.PointF;
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

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class PerspectiveTransformActivity extends AppCompatActivity implements
        HoughResultFragment.HoughTransform, DisplayImageFragment.ImageProcessor,
        BinarizationSettingDialog.DialogListener {

    private Bundle args;

    private List<PointF> pointListResult;
    private Size frameSizeResult;

    private int blurBlockSize = 5;
    private int thresholdBlockSize = 151;
    private double thresholdConstant = 6.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perspective_transform);

        Intent intent = getIntent();
        args = new Bundle();
        args.putString(DisplayImageFragment.ARG_IMAGE_PATH, intent.getStringExtra("photo_path"));

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            HoughResultFragment houghFragment = new HoughResultFragment();
            houghFragment.setArguments(args);

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, houghFragment).commit();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
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
    public int getBlurBlockSize() {
        return blurBlockSize;
    }

    @Override
    public int getThresholdBlockSize() {
        return thresholdBlockSize;
    }

    @Override
    public double getThresholdConstant() {
        return thresholdConstant;
    }

    @Override
    public void onDialogPositiveClick(BinarizationSettingDialog dialog) {
        this.blurBlockSize = dialog.blurBlockSize.getIntProgress();
        this.thresholdBlockSize = dialog.thresholdBlockSize.getIntProgress();
        this.thresholdConstant = dialog.thresholdConstant.getDoubleProgress();

        Fragment f = getSupportFragmentManager().findFragmentByTag("RESULT_FRAGMENT");
        if (f != null) {
            ((DisplayImageFragment) f).processAndDisplay();
        }
    }


    private PointF scale2PointF(Point cvPoint, Point scale) {
        return new PointF((float) ((cvPoint.x + 0.5) * scale.x), (float) ((cvPoint.y + 0.5) * scale.y));
    }

    private Point pointF2cvPoint(PointF pointF) {
        return new Point(pointF.x, pointF.y);
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

    @Override
    public List<PointF> houghTransform(Mat srcBGR, Size frameSize) {

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

        Point topLeft = horizontals.get(0).intersection(verticals.get(0));
        Point topRight = horizontals.get(0).intersection(verticals.get(verticals.size() - 1));
        Point bottomLeft = horizontals.get(horizontals.size() - 1).intersection(verticals.get(0));
        Point bottomRight = horizontals.get(horizontals.size() - 1).intersection(verticals.get(verticals.size() - 1));

        Point scale = new Point(frameSize.width / width / resizeScale, frameSize.height / height / resizeScale);
        List<PointF> pointList = new ArrayList<>(4);
        pointList.add(scale2PointF(topLeft, scale));
        pointList.add(scale2PointF(topRight, scale));
        pointList.add(scale2PointF(bottomLeft, scale));
        pointList.add(scale2PointF(bottomRight, scale));
        return pointList;
    }

    @Override
    public void onHoughResult(List<PointF> pointList, Size frameSize) {
        pointListResult = pointList;
        frameSizeResult = frameSize;

        // Create new fragment
        DisplayImageFragment newFragment = new DisplayImageFragment();
        newFragment.setArguments(args);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, newFragment, "RESULT_FRAGMENT");
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public Mat processImage(Mat srcMat) {
        return pTransform(srcMat, pointListResult, frameSizeResult);
    }

    @Override
    public void onFinishDisplay(Mat dstMat) {
        File photoFile = null;
        String receiptPath = "";
        try {
            photoFile = MainActivity.createImageFile("Receipt", ".pbm", getExternalFilesDir("Receipts"));
            receiptPath = photoFile.getAbsolutePath();
        } catch (IOException ex) {
            // Error occurred while creating the File
            // Log.d(TAG, "Cannot create file");
        }

        // Continue only if the File was successfully created
        if (photoFile != null) {
            Imgcodecs.imwrite(receiptPath, dstMat);
            Intent data = new Intent();
            data.setData(Uri.parse(receiptPath));
            setResult(RESULT_OK, data);
            finish();
        }
    }

    private Mat pTransform(Mat srcBGR, List<PointF> pointList, Size frameSize) {

        /* perspective transformation */
        double xScale = srcBGR.width() / frameSize.width;
        double yScale = srcBGR.height() / frameSize.height;

        // find intersection points
        Point topLeft = pointF2cvPoint(pointList.get(0));
        Point topRight = pointF2cvPoint(pointList.get(1));
        Point bottomLeft = pointF2cvPoint(pointList.get(2));
        Point bottomRight = pointF2cvPoint(pointList.get(3));

        // define the destination image size
        int avgWidth = (int) ((-topLeft.x + topRight.x - bottomLeft.x + bottomRight.x) * xScale / 2);
        int avgHeight = (int) ((-topLeft.y - topRight.y + bottomLeft.y + bottomRight.y) * yScale / 2);
        Mat dstBGR = Mat.zeros(avgHeight, avgWidth, CvType.CV_8UC3);

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
                (topLeft.x + 0.5) * xScale, (topLeft.y + 0.5) * yScale,
                (topRight.x + 0.5) * xScale, (topRight.y + 0.5) * yScale,
                (bottomLeft.x + 0.5) * xScale, (bottomLeft.y + 0.5) * yScale,
                (bottomRight.x + 0.5) * xScale, (bottomRight.y + 0.5) * yScale);

        // get transformation matrix
        Mat transMat = Imgproc.getPerspectiveTransform(srcPoints, dstPoints);

        // apply perspective transformation
        Imgproc.warpPerspective(srcBGR, dstBGR, transMat, dstBGR.size());

        // eliminate boundary
        int xMargin = (int) Math.ceil(xScale * 3);
        int yMargin = (int) Math.ceil(yScale * 3);
        Mat subMat = dstBGR.submat(yMargin, avgHeight - yMargin, xMargin, avgWidth - xMargin);

        // convert to grayscale image
        Mat dst = new Mat();
        Imgproc.cvtColor(subMat, dst, Imgproc.COLOR_BGR2GRAY);

        //Imgproc.threshold(gray, dst, 0, 255, Imgproc.THRESH_OTSU);

        Imgproc.medianBlur(dst, dst, blurBlockSize);
        Imgproc.adaptiveThreshold(dst, dst, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, thresholdBlockSize, thresholdConstant);

        return dst;
    }
}