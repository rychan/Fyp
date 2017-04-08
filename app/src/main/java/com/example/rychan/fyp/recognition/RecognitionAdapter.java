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

import java.util.List;

/**
 * Created by rychan on 17年2月12日.
 */

public class RecognitionAdapter extends ArrayAdapter<RecognitionResult>{

    private Mat receiptImage;

    public RecognitionAdapter(Context context, List<RecognitionResult> resultList, Mat srcImage) {
        super(context, R.layout.listitem_null, resultList);
        this.receiptImage = srcImage;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {

        ViewHolder holder;
        int type = getItemViewType(position);

        if (convertView == null) {
            holder = new ViewHolder();
            switch (type) {
                case RecognitionResult.TYPE_NULL:
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_null, parent, false);
                    holder.imageView = (ImageView) convertView.findViewById(R.id.image);
                    holder.result1 = (TextView) convertView.findViewById(R.id.result);
                    holder.result2 = null;
                    break;
                case RecognitionResult.TYPE_ITEM:
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_item, parent, false);
                    holder.imageView = (ImageView) convertView.findViewById(R.id.image);
                    holder.result1 = (TextView) convertView.findViewById(R.id.itemName);
                    holder.result2 = (TextView) convertView.findViewById(R.id.itemPrice);
                    break;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        DisplayImageFragment.displayImage(receiptImage.rowRange(getItem(position).rowRange), holder.imageView);
        holder.result1.setText(getItem(position).result1);
        if (holder.result2 != null) {
            holder.result2.setText(getItem(position).result2);
        }
        return convertView;
    }

    private static class ViewHolder {
        private ImageView imageView;
        private TextView result1;
        private TextView result2;
    }
}
