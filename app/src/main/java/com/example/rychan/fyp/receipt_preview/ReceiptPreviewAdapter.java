package com.example.rychan.fyp.receipt_preview;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.rychan.fyp.R;
import com.example.rychan.fyp.perspective_transform.DisplayImageFragment;
import com.example.rychan.fyp.provider.Contract.*;

import org.opencv.core.Mat;
import org.opencv.core.Range;

/**
 * Created by rychan on 17年2月12日.
 */

public class ReceiptPreviewAdapter extends CursorAdapter implements View.OnClickListener {

    private Mat src;

    public ReceiptPreviewAdapter(Context context, Cursor c, Mat src) {
        super(context, c, 0);
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
                v = LayoutInflater.from(context).inflate(R.layout.listitem_receipt_other, parent, false);
                holder1 = new TypeOtherHolder();
                holder1.imageView = (ImageView) v.findViewById(R.id.image_view);
                v.setTag(holder1);
                break;

            case ItemEntry.TYPE_ITEM:
                TypeItemHolder holder2;
                v = LayoutInflater.from(context).inflate(R.layout.listitem_receipt_item, parent, false);
                holder2 = new TypeItemHolder();
                holder2.imageView = (ImageView) v.findViewById(R.id.image_view);
                holder2.itemName = (TextView) v.findViewById(R.id.item_name);
                holder2.price = (TextView) v.findViewById(R.id.price);
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
                holder1.imageView.setId(cursor.getPosition());
                holder1.imageView.setOnClickListener(this);
                break;

            case ItemEntry.TYPE_ITEM:
                TypeItemHolder holder2 = (TypeItemHolder) view.getTag();

                DisplayImageFragment.displayImage(src.rowRange(r), holder2.imageView);
                holder2.imageView.setId(cursor.getPosition());
                holder2.imageView.setOnClickListener(this);

                holder2.itemName.setText(cursor.getString(cursor.getColumnIndex(ItemEntry.COLUMN_NAME)));
                holder2.itemName.setId(cursor.getPosition());
                holder2.itemName.setOnClickListener(this);

                holder2.price.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndex(ItemEntry.COLUMN_PRICE))));
                holder2.price.setId(cursor.getPosition());
                holder2.price.setOnClickListener(this);
                break;

            default:
        }
    }

    @Override
    public void onClick(View v) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(v.getId());
        int itemId = cursor.getInt(cursor.getColumnIndex(ItemEntry._ID));
        if (cursor.getInt(cursor.getColumnIndex(ItemEntry.COLUMN_TYPE)) == ItemEntry.TYPE_ITEM &&
                v instanceof ImageView) {
            ContentValues values = new ContentValues();
            values.put(ItemEntry.COLUMN_TYPE, ItemEntry.TYPE_OTHER);
            mContext.getContentResolver().update(
                    ContentUris.withAppendedId(ReceiptProvider.ITEM_CONTENT_URI, itemId),
                    values, null, null);
        } else {
            DialogFragment dialog = new UpdateItemDialog();
            Bundle arg = new Bundle();
            arg.putInt(UpdateItemDialog.ARG_ROW_ID, v.getId());
            dialog.setArguments(arg);
            dialog.show(((AppCompatActivity) mContext).getSupportFragmentManager(), "UpdateItemDialog");
        }
    }

    private static class TypeOtherHolder {
        private ImageView imageView;
    }

    private static class TypeItemHolder {
        private ImageView imageView;
        private TextView itemName;
        private TextView price;
    }
}