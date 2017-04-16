package com.example.rychan.fyp.ReceiptPreview;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.rychan.fyp.R;
import com.example.rychan.fyp.perspective_transform.DisplayImageFragment;
import com.example.rychan.fyp.provider.Contract.*;

import org.opencv.core.Mat;
import org.opencv.core.Range;

/**
 * Created by rychan on 17年2月12日.
 */

public class ReceiptPreviewAdapter extends CursorAdapter {
    private Mat src;

    public ReceiptPreviewAdapter(Context context, Cursor c, Mat src) {
        super(context, c, FLAG_REGISTER_CONTENT_OBSERVER);
        this.src = src;
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return cursor.getInt(cursor.getColumnIndex(ItemEntry.COLUMN_TYPE));
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = null;
        switch (cursor.getInt(cursor.getColumnIndex(ItemEntry.COLUMN_TYPE))) {
            case ItemEntry.TYPE_OTHER:
                TypeOtherHolder holder1;
                v = LayoutInflater.from(context).inflate(R.layout.listitem_preview_other, parent, false);
                holder1 = new TypeOtherHolder();
                holder1.imageView = (ImageView) v.findViewById(R.id.image);
                //holder1.textView = (TextView) convertView.findViewById(R.id.result);
                v.setTag(holder1);
                break;

            case ItemEntry.TYPE_ITEM:
                TypeItemHolder holder2;
                v = LayoutInflater.from(context).inflate(R.layout.listitem_preview_item, parent, false);
                holder2 = new TypeItemHolder();
                holder2.imageView = (ImageView) v.findViewById(R.id.image);
                holder2.itemName = (EditText) v.findViewById(R.id.itemName);
                holder2.itemPrice = (EditText) v.findViewById(R.id.itemPrice);
                v.setTag(holder2);
                break;

            default:
        }
        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Range r = new Range(cursor.getInt(cursor.getColumnIndex(ItemEntry.COLUMN_START_ROW)),
                cursor.getInt(cursor.getColumnIndex(ItemEntry.COLUMN_END_ROW)));

        switch (cursor.getInt(cursor.getColumnIndex(ItemEntry.COLUMN_TYPE))) {
            case ItemEntry.TYPE_OTHER:
                TypeOtherHolder holder1 = (TypeOtherHolder) view.getTag();

                DisplayImageFragment.displayImage(src.rowRange(r), holder1.imageView);
                //holder1.imageView.setId(position);
                //holder1.imageView.setOnClickListener(this);
                break;

            case ItemEntry.TYPE_ITEM:
                TypeItemHolder holder2 = (TypeItemHolder) view.getTag();

                DisplayImageFragment.displayImage(src.rowRange(r), holder2.imageView);
//                holder2.imageView.setId(position);
//                holder2.imageView.setOnClickListener(this);

                holder2.itemName.setText(cursor.getString(cursor.getColumnIndex(ItemEntry.COLUMN_TEXT)));
//                holder2.itemName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//                    @Override
//                    public void onFocusChange(View v, boolean hasFocus) {
//                        if (hasFocus) {
//                            Log.d("Gain Focus", String.valueOf(position)+"left");
////                                currentFocusRow = position;
////                                currentFocusItem = 0;
//                        } else {
//                            Log.d("Lost Focus", String.valueOf(position)+"left");
//                            result.text = ((EditText) v).getText().toString();
//                            recognitionResult.computeTotal();
//                        }
//                    }
//                });

                holder2.itemPrice.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndex(ItemEntry.COLUMN_PRICE))));
//                holder2.itemPrice.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//                    @Override
//                    public void onFocusChange(View v, boolean hasFocus) {
//                        if (hasFocus) {
//                            Log.d("Gain Focus", String.valueOf(position)+"right");
////                                currentFocusRow = position;
////                                currentFocusItem = 1;
//                        } else {
//                            Log.d("Lost Focus", String.valueOf(position)+"right");
//                            result.price = new BigDecimal(((EditText) v).getText().toString());
//                            recognitionResult.computeTotal();
//                        }
//                    }
//                });
                break;
            default:

        }
    }

//    @Override
//    public void onClick(View v) {
//        LineResult item = getItem(v.getId());
//        if (item.type == RecognitionResult.TYPE_ITEM) {
//            item.type = RecognitionResult.TYPE_NULL;
//            recognitionResult.computeTotal();
//            notifyDataSetChanged();
//        } else {
//            item.type = RecognitionResult.TYPE_ITEM;
//            recognitionResult.computeTotal();
//            notifyDataSetChanged();
//        }
//    }

    private static class TypeOtherHolder {
        private ImageView imageView;
    }

    private static class TypeItemHolder {
        private ImageView imageView;
        private EditText itemName;
        private EditText itemPrice;
    }
}