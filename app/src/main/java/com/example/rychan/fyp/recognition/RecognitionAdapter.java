package com.example.rychan.fyp.recognition;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.rychan.fyp.perspective_transform.DisplayImageFragment;
import com.example.rychan.fyp.R;

import org.opencv.core.Mat;
import org.opencv.core.Range;

import java.util.List;

/**
 * Created by rychan on 17年2月12日.
 */

public class RecognitionAdapter extends ArrayAdapter<Range>{

    private Mat receiptImage;
    private List<String> resultList;

    public RecognitionAdapter(Context context, List<Range> rangeList, Mat srcImage, List<String> resultList) {
        super(context, R.layout.list_recognition, rangeList);
        this.receiptImage = srcImage;
        this.resultList = resultList;
    }

    @Override
    public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {

        Range range = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_recognition, parent, false);
        }

        ImageView imageView = (ImageView) convertView.findViewById(R.id.image);
        DisplayImageFragment.displayImage(receiptImage.rowRange(range), imageView);

        TextView textView = (TextView) convertView.findViewById(R.id.recognition_result);
        textView.setText(resultList.get(position));

        return convertView;
    }
}
