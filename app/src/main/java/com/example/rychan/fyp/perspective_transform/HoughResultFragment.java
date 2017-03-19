package com.example.rychan.fyp.perspective_transform;

import android.content.Context;
import android.graphics.PointF;
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
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.List;

public class HoughResultFragment extends DisplayImageFragment {

    private HoughTransform mListener;

    private RegionSelector regionSelector;
    private Size frameSize;

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

        imageView = (ImageView) view.findViewById(R.id.imageView);
        final Mat srcMat = Imgcodecs.imread(imagePath);

        regionSelector = (RegionSelector) view.findViewById(R.id.regionSelector);

        displayImage(srcMat, imageView);
        ViewTreeObserver vto = imageView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                int padding = (int) getResources().getDimension(R.dimen.scanPadding);
                int frameWidth = imageView.getWidth() - 2 * padding;
                int frameHeight = imageView.getHeight() - 2 * padding;
                frameSize = new Size(frameWidth, frameHeight);

                List<PointF> pointList = mListener.houghTransform(srcMat, frameSize);
                regionSelector.setPoints(pointList, frameWidth, frameHeight);
                regionSelector.setVisibility(View.VISIBLE);

                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        frameWidth + 2 * padding, frameHeight + 2 * padding);
                layoutParams.gravity = Gravity.CENTER;
                regionSelector.setLayoutParams(layoutParams);
            }
        });
        return view;
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
                mListener.onHoughResult(regionSelector.getPoints(), frameSize);
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
        List<PointF> houghTransform(Mat srcMat, Size frameSize);
        void onHoughResult(List<PointF> pointList, Size frameSize);
    }
}
