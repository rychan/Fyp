package com.example.rychan.fyp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.rychan.fyp.receipt_preview.ReceiptPreviewActivity;
import com.example.rychan.fyp.perspective_transform.PerspectiveTransformActivity;
import com.example.rychan.fyp.provider.Contract.*;
import com.example.rychan.fyp.xml_preview.XmlPreviewActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements
        View.OnClickListener, AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, DeleteReceiptDialog.DialogListener,
        LoaderManager.LoaderCallbacks<Cursor>{

    // Log
    private static final String TAG = "MainActivity";

    private static final int LIST_ITEM_LOADER = 0;

    private String photoPath;
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int REQUEST_XML_PREVIEW = 3;
    private static final int REQUEST_RECOGNITION = 4;

    private SimpleCursorAdapter simpleCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        simpleCursorAdapter = new SimpleCursorAdapter(
                this,
                R.layout.listitem_main,
                null,
                new String[]{
                        ReceiptEntry.COLUMN_DATE,
                        ReceiptEntry.COLUMN_STATUS,
                        ReceiptEntry.COLUMN_SHOP,
                        ReceiptEntry.COLUMN_TOTAL},
                new int[]{R.id.date, R.id.status, R.id.shop, R.id.total},
                0);
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(simpleCursorAdapter);
        listView.setOnItemClickListener(this);
        listView.setLongClickable(true);
        listView.setOnItemLongClickListener(this);
        getSupportLoaderManager().initLoader(LIST_ITEM_LOADER, null, this);

        Button galleryButton = (Button) findViewById(R.id.gallery_button);
        galleryButton.setOnClickListener(this);
        Button cameraButton = (Button) findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(this);
        Button xmlButton = (Button) findViewById(R.id.export_button);
        xmlButton.setOnClickListener(this);
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "Opencv loaded successfully");
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LIST_ITEM_LOADER:
                return new CursorLoader(this,
                        ReceiptProvider.RECEIPT_CONTENT_URI,
                        null, null, null, ReceiptEntry._ID + " DESC");
            default:
                throw new IllegalArgumentException("Unknown loader id");
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LIST_ITEM_LOADER:
                simpleCursorAdapter.swapCursor(data);
            default:
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LIST_ITEM_LOADER:
                simpleCursorAdapter.swapCursor(null);
            default:
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Cursor cursor = simpleCursorAdapter.getCursor();
        if (cursor.moveToPosition(i)) {
            if (cursor.getInt(cursor.getColumnIndex(ReceiptEntry.COLUMN_STATUS)) != ReceiptEntry.STATUS_PROCESSING) {
                int receiptId = cursor.getInt(cursor.getColumnIndex(ReceiptEntry._ID));
                String shop = cursor.getString(cursor.getColumnIndex(ReceiptEntry.COLUMN_SHOP));
                String date = cursor.getString(cursor.getColumnIndex(ReceiptEntry.COLUMN_DATE));
                double total = cursor.getDouble(cursor.getColumnIndex(ReceiptEntry.COLUMN_TOTAL));
                String receiptPath = cursor.getString(cursor.getColumnIndex(ReceiptEntry.COLUMN_FILE));

                dispatchReceiptPreviewIntent(receiptId, shop, date, total, receiptPath);
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        Cursor cursor = simpleCursorAdapter.getCursor();
        if (cursor.moveToPosition(i)) {
            if (cursor.getInt(cursor.getColumnIndex(ReceiptEntry.COLUMN_STATUS)) != ReceiptEntry.STATUS_PROCESSING) {
                DialogFragment dialogFragment = new DeleteReceiptDialog();
                Bundle arg = new Bundle();
                arg.putString(DeleteReceiptDialog.ARG_FILE_PATH,
                        cursor.getString(cursor.getColumnIndex(ReceiptEntry.COLUMN_FILE)));
                arg.putInt(DeleteReceiptDialog.ARG_RECEIPT_ID,
                        cursor.getInt(cursor.getColumnIndex(ReceiptEntry._ID)));
                dialogFragment.setArguments(arg);
                dialogFragment.show(getSupportFragmentManager(), "delete_receipt");
            }
        }
        return true;
    }

    @Override
    public void onDialogPositiveClick(String filePath, int receiptId) {
        File file = new File(filePath);
        if (!file.delete()) {
            Toast.makeText(this, "Unable to delete binary image file", Toast.LENGTH_LONG).show();
        } else {
            getContentResolver().delete(
                    ContentUris.withAppendedId(ReceiptProvider.RECEIPT_CONTENT_URI, receiptId),
                    null, null);
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.camera_button:
                dispatchCameraIntent();
                break;
            case R.id.gallery_button:
                dispatchGalleryIntent();
                break;
            case R.id.export_button:
                dispatchXmlPreviewIntent();
            default:
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_CAMERA:
                if (resultCode == RESULT_OK) {
                    dispatchPerspectiveTransformIntent(photoPath);
                } else if (resultCode == RESULT_CANCELED) {
                    File file = new File(photoPath);
                    file.delete();
                }
                break;

            case REQUEST_GALLERY:
                if (resultCode == RESULT_OK && data != null) {
                    photoPath = getImagePath(data.getData());
                    dispatchPerspectiveTransformIntent(photoPath);
                }
                break;

            case REQUEST_XML_PREVIEW:
                if (resultCode == RESULT_OK && data != null) {
                    dispatchXmlExportIntent(data.getStringExtra("xml_path"));
                }

            default:
        }
    }

    public static File createFile(String prefix, String format, File storageDir) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyymmdd_hhmmss").format(new Date());
        String imageFileName = prefix + "_" + timeStamp + "_";
        return File.createTempFile(imageFileName, format, storageDir);
    }

    private void dispatchCameraIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/Fyp");
                path.mkdirs();
                photoFile = createFile("Photo", ".jpg", path);
                photoPath = photoFile.getAbsolutePath();
            } catch (IOException ex) {
                Log.d(TAG, "Cannot create file");
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(cameraIntent, REQUEST_CAMERA);
            }
        }
    }

    private void dispatchGalleryIntent() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");

        // Start the Intent
        startActivityForResult(galleryIntent, REQUEST_GALLERY);
    }

    private void dispatchPerspectiveTransformIntent(String receiptPath) {
        Intent perspectiveTransformIntent = new Intent(this, PerspectiveTransformActivity.class);
        perspectiveTransformIntent.putExtra("receipt_path", receiptPath);

        startActivity(perspectiveTransformIntent);
    }

//    private void dispatchRecognitionIntent(String receiptPath) {
//        Intent recognitionIntent = new Intent(this, ReceiptPreviewActivity.class);
//        recognitionIntent.putExtra("receipt_path", receiptPath);
//
//        startActivityForResult(recognitionIntent, REQUEST_RECOGNITION);
//    }

    private void dispatchReceiptPreviewIntent(int receiptId, String shop, String date, double total, String receiptPath) {
        Intent receiptPreviewIntent = new Intent(this, ReceiptPreviewActivity.class);
        receiptPreviewIntent.putExtra("receipt_id", receiptId);
        receiptPreviewIntent.putExtra("shop", shop);
        receiptPreviewIntent.putExtra("date", date);
        receiptPreviewIntent.putExtra("total", total);
        receiptPreviewIntent.putExtra("file", receiptPath);

        ContentValues values = new ContentValues();
        values.put(ReceiptEntry.COLUMN_STATUS, ReceiptEntry.STATUS_OLD);
        getContentResolver().update(
                ContentUris.withAppendedId(ReceiptProvider.RECEIPT_CONTENT_URI, receiptId),
                values, null, null);

        startActivity(receiptPreviewIntent);
    }

    private void dispatchXmlPreviewIntent() {
        Intent xmlPreviewIntent = new Intent(this, XmlPreviewActivity.class);

        startActivityForResult(xmlPreviewIntent, REQUEST_XML_PREVIEW);
    }

    private void dispatchXmlExportIntent(String xmlPath) {
        Intent xmlExportIntent = new Intent(Intent.ACTION_SEND);
        xmlExportIntent.setType("application/xml");
        xmlExportIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + xmlPath));

        startActivity(xmlExportIntent);
    }


    public String getImagePath(Uri uri){
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":")+1);
        cursor.close();

        cursor = getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        // Save state members to saved instance
        savedInstanceState.putString("photo_path", photoPath);

        // Always call the superclass so it can save the view hierarchy
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance
        photoPath = savedInstanceState.getString("photo_path");
    }


//    // Button state
//    private static final int STATE_INIT = 0;
//    private static final int STATE_HOUGH_TRANSFORM = 1;
//    private static final int STATE_PERSPECTIVE_TRANSFORM = 2;
//    private static final int STATE_MSER_DETECTOR = 3;
//    private static final int STATE_TEXT_DETECTION = 4;
//    private static final int STATE_RECOGNITION = 5;
//    private static final int STATE_REPEAT = 6;
//    private static final String[] STATE_LABEL = {
//            "Choose one from below", "Hough transform",
//            "Perspective transform", "MSER detector",
//            "Text detect", "Text XmlPreviewActivity",
//            "Repeat or choose one from below"};
//    private int buttonState = STATE_INIT;
//
//    ////////////////////////////////////////////////////////////////////////////////////////////////
//    private Mat mat2;
//    private Mat mat1;
//
//    class Line {
//        Point pt1;
//        Point pt2;
//        Point midPt;
//
//        Line(Point myPt1, Point myPt2) {
//            pt1 = myPt1;
//            pt2 = myPt2;
//            midPt = new Point((pt1.x + pt2.x)/2, (pt1.y + pt2.y)/2);
//        }
//
//        Point intersection(Line line){
//            double x1 = pt1.x, y1 = pt1.y;
//            double x2 = pt2.x, y2 = pt2.y;
//            double x3 = line.pt1.x, y3 = line.pt1.y;
//            double x4 = line.pt2.x, y4 = line.pt2.y;
//            double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
//
//            if (d == 0) {
//                return new Point(-1, -1);
//            } else {
//                Point pt = new Point();
//                pt.x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
//                pt.y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
//                return pt;
//            }
//        }
//
//        void draw(Mat img, double scale){
//            Scalar colour = new Scalar(0,0,255);
//            Imgproc.line(img, new Point((pt1.x + 0.5) * scale, (pt1.y + 0.5) * scale),
//                    new Point((pt2.x + 0.5) * scale, (pt2.y + 0.5) * scale), colour, (int) scale / 2);
//        }
//    }
//
//    class xComparator implements Comparator<Line> {
//        @Override
//        public int compare(Line l1, Line l2) {
//            return (l1.midPt.x < l2.midPt.x) ? -1 : (l1.midPt.x ==  l2.midPt.x) ? 0 : 1;
//        }
//    }
//
//    class yComparator implements Comparator<Line> {
//        @Override
//        public int compare(Line l1, Line l2) {
//            return (l1.midPt.y < l2.midPt.y) ? -1 : (l1.midPt.y ==  l2.midPt.y) ? 0 : 1;
//        }
//    }
//
//    private Mat pTransform(Mat srcBGR, Mat canny) {
//
//        // resize mat2 to resizeBGR to reduce computation
//        Mat resizeBGR = new Mat();
//        double width = srcBGR.cols(), height = srcBGR.rows();
//        double minWidth = 200;
//        double scale = Math.min(8.0, width / minWidth);
//        double resizeWidth = width / scale, resizeHeight = height / scale;
//        Imgproc.resize(srcBGR, resizeBGR, new Size(resizeWidth, resizeHeight), 0 , 0, Imgproc.INTER_AREA);
//
//
////        Mat resizeBGR = srcBGR.clone();
////        double scale = 1.0;
////        double resizeWidth = resizeBGR.cols(), resizeHeight = resizeBGR.rows();
//        //Mat imgDis = resizeBGR.clone();
//
//        // convert to grayscale image
//        Mat gray = new Mat();
//        Imgproc.cvtColor(resizeBGR, gray, Imgproc.COLOR_BGR2GRAY);
//
//        // get edges of the image
//        double highThreshold = Imgproc.threshold(gray, new Mat(), 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
//        double lowThreshold = highThreshold * 0.5;
//        Imgproc.Canny(gray, canny, lowThreshold, highThreshold);
//
//        Mat lines = new Mat();
//        Imgproc.HoughLinesP(canny, lines, 1, Math.PI / 180, (int) resizeWidth/3 , resizeWidth / 3, 20);
//
//        List<Line> horizontals = new ArrayList<>();
//        List<Line> verticals = new ArrayList<>();
//        for (int i = 0; i < lines.rows(); ++i) {
//            double[] v = lines.get(i,0);
//            double deltaX = v[0] - v[2];
//            double deltaY = v[1] - v[3];
//            Line l = new Line(new Point(v[0], v[1]), new Point(v[2], v[3]));
//
//            // get horizontal lines and vertical lines respectively
//            if (Math.abs(deltaX) > Math.abs(deltaY)) {
//                horizontals.add(l);
//            } else {
//                verticals.add(l);
//            }
//        }
//
//        // handle cases when not enough lines are detected
//        if (horizontals.size() < 2) {
//            if (horizontals.size() == 0 || horizontals.get(0).midPt.y > resizeHeight / 2) {
//                horizontals.add(new Line(new Point(0, 0), new Point(resizeWidth - 1, 0)));
//            }
//            if (horizontals.size() == 0 || horizontals.get(0).midPt.y <= resizeHeight / 2) {
//                horizontals.add(new Line(new Point(0, resizeHeight - 1), new Point(resizeWidth - 1, resizeHeight - 1)));
//            }
//        }
//        if (verticals.size() < 2) {
//            if (verticals.size() == 0 || verticals.get(0).midPt.x > resizeWidth / 2) {
//                verticals.add(new Line(new Point(0, 0), new Point(0, resizeHeight - 1)));
//            }
//            if (verticals.size() == 0 || verticals.get(0).midPt.x <= resizeWidth / 2) {
//                verticals.add(new Line(new Point(resizeWidth - 1, 0), new Point(resizeWidth - 1, resizeHeight - 1)));
//            }
//        }
//
//        // sort lines according to their center point
//        Collections.sort(horizontals, new yComparator());
//        Collections.sort(verticals, new xComparator());
//
//
//        /* perspective transformation */
//
//        // find intersection points
//        Point topLeft = horizontals.get(0).intersection(verticals.get(0));
//        Point topRight = horizontals.get(0).intersection(verticals.get(verticals.size()-1));
//        Point bottomLeft = horizontals.get(horizontals.size()-1).intersection(verticals.get(0));
//        Point bottomRight = horizontals.get(horizontals.size()-1).intersection(verticals.get(verticals.size()-1));
//
//        // define the destination image size
//        int avgWidth = (int) ((- topLeft.x + topRight.x - bottomLeft.x + bottomRight.x) * scale / 2);
//        int avgHeight = (int) ((- topLeft.y - topRight.y + bottomLeft.y + bottomRight.y) * scale / 2);
//        Mat dstBGR = Mat.zeros(avgHeight, avgWidth, CvType.CV_8UC3);
//
//        // find corners of destination image with the sequence [topLeft, topRight, bottomLeft, bottomRight]
//        Mat dstPoints = new Mat(4,1,CvType.CV_32FC2);
//        dstPoints.put(0, 0,
//                0.0, 0.0,
//                avgWidth - 1, 0,
//                0, avgHeight - 1,
//                avgWidth - 1, avgHeight - 1);
//
//        // find corners of source image with the sequence [topLeft, topRight, bottomLeft, bottomRight]
//        Mat srcPoints = new Mat(4,1,CvType.CV_32FC2);
//        srcPoints.put(0, 0,
//                (topLeft.x + 0.5) * scale, (topLeft.y + 0.5) * scale,
//                (topRight.x + 0.5) * scale, (topRight.y + 0.5) * scale,
//                (bottomLeft.x + 0.5) * scale, (bottomLeft.y + 0.5) * scale,
//                (bottomRight.x + 0.5) * scale, (bottomRight.y + 0.5) * scale);
//
//        // get transformation matrix
//        Mat transMat = Imgproc.getPerspectiveTransform(srcPoints, dstPoints);
//
//        // apply perspective transformation
//        Imgproc.warpPerspective(srcBGR, dstBGR, transMat, dstBGR.size());
//
//        // change input matrix for debug
//        for (Line line: horizontals) {
//            line.draw(srcBGR, scale);
//        }
//        for (Line line: verticals) {
//            line.draw(srcBGR, scale);
//        }
//
//        return dstBGR;
//    }
//
//
//    private Mat detectText(Mat srcBgr) {
//        int height = srcBgr.height();
//        int width = srcBgr.width();
////        int size = height * width;
//
//        Scalar CONTOUR_COLOR = new Scalar(255);
//
//        Mat srcGray = new Mat();
//        Imgproc.cvtColor(srcBgr, srcGray, Imgproc.COLOR_BGR2GRAY);
//
//        // Apply MSER detector to the grey scale image
//        MatOfKeyPoint pointMat = new MatOfKeyPoint();
//        FeatureDetector detector = FeatureDetector.create(FeatureDetector.MSER);
//        detector.detect(srcGray, pointMat);
//        List<KeyPoint> pointList = pointMat.toList();
//
//        Mat test = new Mat();
//        Features2d.drawKeypoints(srcGray, pointMat, test, CONTOUR_COLOR, Features2d.DRAW_RICH_KEYPOINTS
//        );
//
//        // Loop through all key points and draw valid rectangles on a mask
//        KeyPoint point;
//        Mat mask = Mat.zeros(srcGray.size(), CvType.CV_8UC1);
//
//        for (int ind = 0; ind < pointList.size(); ++ind) {
//            point = pointList.get(ind);
//            int xStart = Math.max((int) (point.pt.x - 0.5 * point.size), 1);
//            int yStart = Math.max((int) (point.pt.y - 0.5 * point.size), 1);
//            int xLength = Math.min((int) (point.size), width - xStart);
//            int yLength = Math.min((int) (point.size), height - yStart);
//
//            Rect rect = new Rect(xStart, yStart, xLength, yLength);
//            Mat roi = new Mat(mask, rect);
//            roi.setTo(CONTOUR_COLOR);
//        }
//
//        // Do Dilation on the rectangles
//        Mat kernel = new Mat(5, 20, CvType.CV_8UC1, Scalar.all(255));
//        Mat dilatedMask = new Mat();
//        Imgproc.morphologyEx(mask, dilatedMask, Imgproc.MORPH_DILATE, kernel);
//
//        // Group rectangles together
//        List<MatOfPoint> contour = new ArrayList<>();
//        Imgproc.findContours(dilatedMask, contour, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
//
//        // Reject rectangles that are too big , too small or too tall
//        for (int ind = 0; ind < contour.size(); ind++) {
//            Rect rect = Imgproc.boundingRect(contour.get(ind));
////            if (rect.area() < 100 || rect.width / rect.height < 2) {
////                Mat roi = new Mat(dilatedMask, rect);
////                roi.setTo(new Scalar(0, 0, 0));
////            } else
//            Imgproc.rectangle(srcBgr, rect.br(), rect.tl(), CONTOUR_COLOR);
//        }
//        return mask;
////        return test;
////        return dilatedMask;
//    }
//
//
//    private Mat segmentation(Mat srcBGR) {
//
//        int width = srcBGR.cols();
//        int height = srcBGR.rows();
//
//        Mat gray = new Mat();
//        Imgproc.cvtColor(srcBGR, gray, Imgproc.COLOR_BGR2GRAY);
//
//        // get edges of the image
//        double highThreshold = Imgproc.threshold(gray, new Mat(), 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
//        double lowThreshold = highThreshold * 0.5;
//        Mat canny = new Mat();
//        Imgproc.Canny(gray, canny, lowThreshold, highThreshold);
//
//        Mat rowSum = new Mat();
//        Core.reduce(canny, rowSum, 1, Core.REDUCE_SUM, CvType.CV_32S);
//        rowSum.convertTo(rowSum, CvType.CV_8U, 1.0/width);
//
//        Mat rowMask = new Mat();
//        Imgproc.threshold(rowSum, rowMask, 5, 255, Imgproc.THRESH_BINARY);
//
//        Mat kernel = new Mat(9, 1, CvType.CV_8UC1, Scalar.all(255));
//        Imgproc.morphologyEx(rowMask, rowMask, Imgproc.MORPH_DILATE, kernel);
//
//        Mat mask = new Mat(height, width, CvType.CV_8UC1);
//        for (int i = 0; i < width; ++i) {
//            rowMask.copyTo(mask.col(i));
//        }
//
//        List<MatOfPoint> contour = new ArrayList<>();
//        Imgproc.findContours(mask, contour, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
//        for (int i = 0; i < contour.size(); ++i) {
//            Rect rect = Imgproc.boundingRect(contour.get(i));
//            Imgproc.rectangle(srcBGR, rect.br(), rect.tl(), new Scalar(0, 255, 0));
//        }
//
////        srcBGR.setTo(new Scalar(0, 0, 0), mask);
//
//        return canny;
//    }

}