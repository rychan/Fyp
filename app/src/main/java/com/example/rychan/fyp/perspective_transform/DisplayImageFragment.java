package com.example.rychan.fyp.perspective_transform;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.example.rychan.fyp.R;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class DisplayImageFragment extends Fragment implements View.OnClickListener {

    protected static final String ARG_IMAGE_PATH = "image_path";
    protected String imagePath;

    private ImageProcessing mListener;

    protected Button button;
    protected ImageView imageView;

    private Mat dstMat;

    public DisplayImageFragment() {
        // Required empty public constructor
    }

    public static DisplayImageFragment newInstance(String imagePath) {
        DisplayImageFragment fragment = new DisplayImageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_PATH, imagePath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imagePath = getArguments().getString(ARG_IMAGE_PATH);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_display_image, container, false);
        button = (Button) view.findViewById(R.id.button);
        button.setOnClickListener(this);

        imageView = (ImageView) view.findViewById(R.id.imageView);
        Mat srcMat = Imgcodecs.imread(imagePath);
        dstMat = mListener.processImage(srcMat);
        displayImage(dstMat, imageView);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ImageProcessing) {
            mListener = (ImageProcessing) context;
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
                mListener.onFinished(dstMat);
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
    public interface ImageProcessing {
        Mat processImage(Mat srcMat);
        void onFinished(Mat dstMat);
    }

    public static Mat fitScreen(Mat src, int max) {
        Mat dst = new Mat();
        int width = src.cols();
        int height = src.rows();
        if (width > height & width > max) {
            Size size = new Size(max, max * height / width);
            Imgproc.resize(src, dst, size, 0, 0, Imgproc.INTER_AREA);
            return dst;
        } else if (height >= width & height > max){
            Size size = new Size(max * width / height, max);
            Imgproc.resize(src, dst, size, 0, 0, Imgproc.INTER_AREA);
            return dst;
        }
        return src;
    }

    public static void displayImage(Mat src, ImageView iv) {
        Mat m = fitScreen(src, 2048);
        if (m.channels() == 3) {
            Imgproc.cvtColor(m, m, Imgproc.COLOR_BGR2RGB);
        }

        // convert to bitmap:
        Bitmap bm = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(m, bm);

        iv.setImageBitmap(bm);
    }
}
