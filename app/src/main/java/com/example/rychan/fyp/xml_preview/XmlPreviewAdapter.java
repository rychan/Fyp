package com.example.rychan.fyp.xml_preview;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.rychan.fyp.R;
import com.example.rychan.fyp.provider.Contract.*;
import com.example.rychan.fyp.provider.Contract.ItemEntry;


/**
 * Created by rychan on 17年2月12日.
 */

public class XmlPreviewAdapter extends CursorAdapter {
    private static final int TYPE_FIRST = 0;
    private static final int TYPE_OTHER = 1;


    public XmlPreviewAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    private int getItemViewType(Cursor cursor) {
        int currentReceiptId = cursor.getInt(cursor.getColumnIndex(ItemEntry.COLUMN_RECEIPT_ID));
        if (cursor.moveToPrevious()) {
            if (cursor.getInt(cursor.getColumnIndex(ItemEntry.COLUMN_RECEIPT_ID)) == currentReceiptId) {
                return TYPE_OTHER;
            }
            cursor.moveToNext();
        }
        return TYPE_FIRST;
    }

    @Override
    public int getItemViewType(int position) {
        return getItemViewType((Cursor) getItem(position));
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = null;
        switch (getItemViewType(cursor)) {
            case TYPE_FIRST:
                ReceiptHolder holder1;
                v = LayoutInflater.from(context).inflate(R.layout.listitem_xml_receipt, parent, false);
                holder1 = new ReceiptHolder();
                holder1.shop = (TextView) v.findViewById(R.id.shop);
                holder1.date = (TextView) v.findViewById(R.id.date);
                View itemLayout = v.findViewById(R.id.item_layout);
                holder1.itemName = (TextView) itemLayout.findViewById(R.id.item_name);
                holder1.price = (TextView) itemLayout.findViewById(R.id.price);
                v.setTag(holder1);
                break;

            case TYPE_OTHER:
                ItemHolder holder2;
                v = LayoutInflater.from(context).inflate(R.layout.listitem_receipt_item, parent, false);
                holder2 = new ItemHolder();
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
        switch (getItemViewType(cursor)) {
            case ItemEntry.TYPE_OTHER:
                ReceiptHolder holder1 = (ReceiptHolder) view.getTag();
                holder1.shop.setText(cursor.getString(cursor.getColumnIndex(ReceiptEntry.COLUMN_SHOP)));
                holder1.date.setText(cursor.getString(cursor.getColumnIndex(ReceiptEntry.COLUMN_DATE)));
                holder1.itemName.setText(cursor.getString(cursor.getColumnIndex(ItemEntry.COLUMN_TEXT)));
                holder1.price.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndex(ItemEntry.COLUMN_PRICE))));
                break;

            case ItemEntry.TYPE_ITEM:
                ItemHolder holder2 = (ItemHolder) view.getTag();

                holder2.itemName.setText(cursor.getString(cursor.getColumnIndex(ItemEntry.COLUMN_TEXT)));
                holder2.price.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndex(ItemEntry.COLUMN_PRICE))));
                break;
            default:

        }
    }

    private static class ReceiptHolder {
        private TextView shop;
        private TextView date;
        private TextView itemName;
        private TextView price;
    }

    private static class ItemHolder {
        private TextView itemName;
        private TextView price;
    }
}