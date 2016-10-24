package com.example.rychan.fyp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String SD_PATH= Environment.getExternalStorageDirectory().getPath();
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_image);
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "Opencv loaded successfully");
                    mainFun();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onResume()
    {
        Log.i(TAG, "called onResume");
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public void mainFun() {

        Button button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // read image
                editText = (EditText) findViewById(R.id.editText);
                String img = editText.getText().toString();
                Mat mRgba = Imgcodecs.imread(SD_PATH + "/testimages/" + img);
                Mat mGray = Imgcodecs.imread(SD_PATH + "/testimages/" + img, 0);

                Mat mDRgba = new Mat();
                Mat mDGray = new Mat();

                downScale(mRgba,mDRgba);
                downScale(mGray,mDGray);

                textDetection(mDRgba, mDGray);
                displayImage(mDRgba);
            }
        });


        // make a mat and draw something
        Mat m = Mat.zeros(600,400, CvType.CV_8UC3);
        Imgproc.putText(m, "Display image", new Point(10,200), Core.FONT_HERSHEY_SCRIPT_SIMPLEX, 2, new Scalar(200,200,0),2);
        displayImage(m);
    }

    private void displayImage(Mat m) {
        // convert to bitmap:
        Bitmap bm = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(m, bm);

        // find the imageview and draw it!
        ImageView iv = (ImageView) findViewById(R.id.imageView);
        iv.setImageBitmap(bm);
    }


    private void downScale(Mat src, Mat dst) {
        int w = src.cols();
        int h = src.rows();
        while (w > 2048 || h > 2048) {
            w /= 2;
            h /= 2;
        }
        Imgproc.resize(src, dst, new Size(w,h));
    }

    private void textDetection(Mat mRgba, Mat mGrey) {
        Scalar CONTOUR_COLOR = new Scalar(255);
        MatOfKeyPoint keypoint = new MatOfKeyPoint();
        List<KeyPoint> listpoint;
        KeyPoint kpoint;
        Mat mask = Mat.zeros(mGrey.size(), CvType.CV_8UC1);
        int rectanx1;
        int rectany1;
        int rectanx2;
        int rectany2;
        int imgsize = mGrey.height() * mGrey.width();
        Scalar zeos = new Scalar(0, 0, 0);

        List<MatOfPoint> contour2 = new ArrayList<>();
        Mat kernel = new Mat(1, 50, CvType.CV_8UC1, Scalar.all(255));
        Mat morbyte = new Mat();
        Mat hierarchy = new Mat();

        Rect rectan3;

        FeatureDetector detector = FeatureDetector.create(FeatureDetector.MSER);
        detector.detect(mGrey, keypoint);
        listpoint = keypoint.toList();

        for (int ind = 0; ind < listpoint.size(); ind++) {
            kpoint = listpoint.get(ind);
            rectanx1 = (int) (kpoint.pt.x - 0.5 * kpoint.size);
            rectany1 = (int) (kpoint.pt.y - 0.5 * kpoint.size);
            rectanx2 = (int) (kpoint.size);
            rectany2 = (int) (kpoint.size);
            if (rectanx1 <= 0)
                rectanx1 = 1;
            if (rectany1 <= 0)
                rectany1 = 1;
            if ((rectanx1 + rectanx2) > mGrey.width())
                rectanx2 = mGrey.width() - rectanx1;
            if ((rectany1 + rectany2) > mGrey.height())
                rectany2 = mGrey.height() - rectany1;
            Rect rectant = new Rect(rectanx1, rectany1, rectanx2, rectany2);
            try {
                Mat roi = new Mat(mask, rectant);
                roi.setTo(CONTOUR_COLOR);
            } catch (Exception ex) {
                Log.d("mylog", "mat roi error " + ex.getMessage());
            }
        }

        Imgproc.morphologyEx(mask, morbyte, Imgproc.MORPH_DILATE, kernel);

        Imgproc.findContours(morbyte, contour2, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        for (int ind = 0; ind < contour2.size(); ind++) {
            rectan3 = Imgproc.boundingRect(contour2.get(ind));
            if (rectan3.area() > 0.5 * imgsize || rectan3.area() < 100 || rectan3.width / rectan3.height < 2) {
                Mat roi = new Mat(morbyte, rectan3);
                roi.setTo(zeos);

            } else
                Imgproc.rectangle(mRgba, rectan3.br(), rectan3.tl(), CONTOUR_COLOR);
        }
        //return mRgba;
    }
}