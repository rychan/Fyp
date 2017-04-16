package com.example.rychan.fyp.recognition;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.rychan.fyp.perspective_transform.DisplayImageFragment;
import com.example.rychan.fyp.R;
import com.example.rychan.fyp.recognition.RecognitionResult.*;

import java.math.BigDecimal;

/**
 * Created by rychan on 17年2月12日.
 */

public class RecognitionAdapter extends ArrayAdapter<LineResult> implements View.OnClickListener{
    RecognitionResult recognitionResult;

    public RecognitionAdapter(Context context, RecognitionResult recognitionResult) {
        super(context, R.layout.listitem_null, recognitionResult.resultList);
        this.recognitionResult = recognitionResult;
    }

    @Override
    public LineResult getItem(int position) {
        return recognitionResult.resultList.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).type == RecognitionResult.TYPE_ITEM) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public @NonNull View getView(final int position, View convertView, @NonNull ViewGroup parent) {

        int type = getItemViewType(position);
        final LineResult result = getItem(position);

        switch (type) {
            case RecognitionResult.TYPE_ITEM:
                TypeItemHolder holder2;

                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_item, parent, false);
                    holder2 = new TypeItemHolder();
                    holder2.imageView = (ImageView) convertView.findViewById(R.id.image);
                    holder2.itemName = (EditText) convertView.findViewById(R.id.itemName);
                    holder2.itemPrice = (EditText) convertView.findViewById(R.id.itemPrice);
                    convertView.setTag(holder2);
                } else {
                    holder2 = (TypeItemHolder) convertView.getTag();
                }

                if (result != null) {
                    DisplayImageFragment.displayImage(recognitionResult.receiptImage.rowRange(result.rowRange), holder2.imageView);
                    holder2.imageView.setId(position);
                    holder2.imageView.setOnClickListener(this);
                    holder2.itemName.setText(result.text);
                    holder2.itemName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            if (hasFocus) {
                                Log.d("Gain Focus", String.valueOf(position)+"left");
//                                currentFocusRow = position;
//                                currentFocusItem = 0;
                            } else {
                                Log.d("Lost Focus", String.valueOf(position)+"left");
                                result.text = ((EditText) v).getText().toString();
                                recognitionResult.computeTotal();
                            }
                        }
                    });
                    holder2.itemPrice.setText(result.number.toString());
                    holder2.itemPrice.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            if (hasFocus) {
                                Log.d("Gain Focus", String.valueOf(position)+"right");
//                                currentFocusRow = position;
//                                currentFocusItem = 1;
                            } else {
                                Log.d("Lost Focus", String.valueOf(position)+"right");
                                result.number = new BigDecimal(((EditText) v).getText().toString());
                                recognitionResult.computeTotal();
                            }
                        }
                    });
//                    if (currentFocusRow == position) {
//                        if (currentFocusItem == 0) {
//                            holder2.itemName.requestFocus();
//                        } else {
//                            holder2.itemPrice.requestFocus();
//                        }
//                    }
                }
                break;

            default:
                TypeNullHolder holder1;

                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_null, parent, false);
                    holder1 = new TypeNullHolder();
                    holder1.imageView = (ImageView) convertView.findViewById(R.id.image);
                    //holder1.textView = (TextView) convertView.findViewById(R.id.result);
                    convertView.setTag(holder1);
                } else {
                    holder1 = (TypeNullHolder) convertView.getTag();
                }

                if (result != null) {
                    DisplayImageFragment.displayImage(recognitionResult.receiptImage.rowRange(result.rowRange), holder1.imageView);
                    holder1.imageView.setId(position);
                    holder1.imageView.setOnClickListener(this);
                    //holder1.textView.setText(result.text);
                }
        }
        return convertView;
    }

    @Override
    public void onClick(View v) {
        LineResult item = getItem(v.getId());
        if (item.type == RecognitionResult.TYPE_ITEM) {
            item.type = RecognitionResult.TYPE_NULL;
            recognitionResult.computeTotal();
            notifyDataSetChanged();
        } else {
            item.type = RecognitionResult.TYPE_ITEM;
            recognitionResult.computeTotal();
            notifyDataSetChanged();
        }
    }

    private static class TypeNullHolder {
        private ImageView imageView;
        //private TextView textView;
    }

    private static class TypeItemHolder {
        private ImageView imageView;
        private EditText itemName;
        private EditText itemPrice;
    }
}