package com.example.rychan.fyp.perspective_transform;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.rychan.fyp.R;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.ArrayList;
import java.util.List;

public class HoughResultFragment extends DisplayImageFragment {

    private HoughTransform mListener;

    private int selectorCount = 1;
    private RegionSelector[] selectors = new RegionSelector[4];
    private Size frameSize;
    private Point scale;

    public HoughResultFragment() {
        // Required empty public constructor
    }

    public static HoughResultFragment newInstance(String imagePath) {
        HoughResultFragment fragment = new HoughResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_PATH, imagePath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_hough_result, container, false);
        button = (Button) view.findViewById(R.id.button);
        button.setOnClickListener(this);

        imageView = (ImageView) view.findViewById(R.id.image_view);
        final Mat src = Imgcodecs.imread(imagePath);

        selectors[0] = (RegionSelector) view.findViewById(R.id.region_selector1);
        selectors[1] = (RegionSelector) view.findViewById(R.id.region_selector2);
        selectors[2] = (RegionSelector) view.findViewById(R.id.region_selector3);
        selectors[3] = (RegionSelector) view.findViewById(R.id.region_selector4);

        displayImage(src, imageView);
        ViewTreeObserver vto = imageView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                BoundaryDetectionTask boundaryDetectionTask = new BoundaryDetectionTask(getContext(), src);
                boundaryDetectionTask.execute();
            }
        });
        return view;
    }

    public void setVisiblility(int which) {
        switch (which) {
            case 0:
                for (int i = 0; i < selectorCount; ++i) {
                    selectors[i].setVisibility(View.VISIBLE);
                }
                break;
            default:
                for (int i = 0; i < selectorCount; ++i) {
                    if (i == which - 1) {
                        selectors[i].setVisibility(View.VISIBLE);
                    } else {
                        selectors[i].setVisibility(View.GONE);
                    }
                }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof HoughTransform) {
            mListener = (HoughTransform) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnButtonClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                List<List<Point>> boundaryList = new ArrayList<>(selectorCount);
                for (int i = 0; i < selectorCount; ++i) {
                    List<Point> boundary = new  ArrayList<>(4);
                    for (PointF point : selectors[i].getPoints()) {
                        boundary.add(divPointF2CvPoint(point, scale));
                    }
                    boundaryList.add(boundary);
                }
                mListener.onBoundaryDetected(boundaryList);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface HoughTransform {
        List<Rect> detectReceipt(Mat src);
        List<Point> houghTransform(Mat src);
        void onBoundaryDetected(List<List<Point>> boundaryList);
    }

    private class BoundaryDetectionTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progressDialog;
        private Mat src;

        private List<List<PointF>> pointList = new ArrayList<>();
        private int padding;
        private int frameWidth;
        private int frameHeight;

        BoundaryDetectionTask(Context context, Mat src) {
            this.progressDialog = new ProgressDialog(context);
            this.src = src;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Processing...");
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.show();

            padding = (int) getResources().getDimension(R.dimen.scan_padding);
            frameWidth = imageView.getWidth() - 2 * padding;
            frameHeight = imageView.getHeight() - 2 * padding;
            frameSize = new Size(frameWidth, frameHeight);
            scale = new Point(((double) frameWidth) / src.width(), ((double) frameHeight) / src.height());
        }

        @Override
        protected Void doInBackground(Void... voids) {
            List<Rect> subMatList = mListener.detectReceipt(src);
            if (subMatList.isEmpty() || subMatList.size() > 4) {
                throw new IllegalArgumentException("too many or too little receipts detected");
            }
            for (Rect subMat : subMatList) {
                Point offset = subMat.tl();
                List<Point> houghResult = mListener.houghTransform(src.submat(subMat));
                List<PointF> selectorInput = new ArrayList<>(4);
                for (Point point : houghResult) {
                    selectorInput.add(addAndMulCvPoint2PointF(point, offset, scale));
                }
                pointList.add(selectorInput);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
//            displayImage(src, imageView);
            for (selectorCount = 0; selectorCount < pointList.size(); ++selectorCount) {
                selectors[selectorCount].setPoints(pointList.get(selectorCount), frameWidth, frameHeight);
                selectors[selectorCount].setVisibility(View.VISIBLE);

                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        frameWidth + 2 * padding, frameHeight + 2 * padding);
                layoutParams.gravity = Gravity.CENTER;
                selectors[selectorCount].setLayoutParams(layoutParams);
            }
        }
    }

    public static PointF addAndMulCvPoint2PointF(Point cvPoint, Point offset, Point scale) {
        return new PointF((float) ((cvPoint.x + offset.x) * scale.x), (float) ((cvPoint.y + offset.y) * scale.y));
    }

    public static Point divPointF2CvPoint(PointF pointF, Point scale) {
        return new Point((pointF.x + 0.5) / scale.x, (pointF.y + 0.5) / scale.y);
    }
}
