package com.example.rychan.fyp.receipt_preview;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.rychan.fyp.DatePickerDialogFragment;
import com.example.rychan.fyp.R;
import com.example.rychan.fyp.perspective_transform.DisplayImageFragment;
import com.example.rychan.fyp.provider.Contract.*;

import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.imgcodecs.Imgcodecs;


public class ReceiptPreviewActivity extends AppCompatActivity implements
        View.OnClickListener, AdapterView.OnItemClickListener,
        DatePickerDialogFragment.DialogListener, UpdateItemDialog.DialogListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LIST_ITEM_LOADER = 0;
    private static final int AUTO_COMPLETE_LOADER = 1;

    private SimpleCursorAdapter simpleCursorAdapter;
    private ReceiptPreviewAdapter receiptPreviewAdapter;

    private AutoCompleteTextView shopText;
    private TextView dateText;
    private TextView totalText;
    private int receiptId;
    private Mat src;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_preview);
        setUiToHideKeyboard(findViewById(R.id.activity_receipt_preview));

        Intent intent = getIntent();
        receiptId = intent.getIntExtra("receipt_id", -1);
        String shop = intent.getStringExtra("shop");
        String date = intent.getStringExtra("date");
        double total = intent.getDoubleExtra("total", 0);
        String receiptPath = intent.getStringExtra("file");
        src = Imgcodecs.imread(receiptPath, 0);

        shopText = (AutoCompleteTextView) findViewById(R.id.shop);
        shopText.setText(shop);
        simpleCursorAdapter = new SimpleCursorAdapter(this,
                R.layout.listitem_plaintext,
                null,
                new String[]{ReceiptEntry.COLUMN_SHOP},
                new int[]{R.id.text_view},
                0);
        simpleCursorAdapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            @Override
            public CharSequence convertToString(Cursor cursor) {
                return cursor.getString(cursor.getColumnIndex(ReceiptEntry.COLUMN_SHOP));
            }
        });
        shopText.setAdapter(simpleCursorAdapter);
        getSupportLoaderManager().initLoader(AUTO_COMPLETE_LOADER, null, this);

        dateText = (TextView) findViewById(R.id.date);
        dateText.setText(date);
        dateText.setOnClickListener(this);

        totalText = (TextView) findViewById(R.id.total);
        totalText.setText(String.valueOf(total));

        ListView listView = (ListView) findViewById(R.id.list_view);
        receiptPreviewAdapter = new ReceiptPreviewAdapter(this, null, src);
        listView.setAdapter(receiptPreviewAdapter);
        listView.setOnItemClickListener(this);
        getSupportLoaderManager().initLoader(LIST_ITEM_LOADER, null, this);

        Button saveButton = (Button) findViewById(R.id.button);
        saveButton.setOnClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LIST_ITEM_LOADER:
                return new CursorLoader(this,
                        ReceiptProvider.ITEM_CONTENT_URI,
                        new String[]{ItemEntry.DATABASE_TABLE_NAME + "." + ItemEntry._ID,
                                ItemEntry.COLUMN_NAME, ItemEntry.COLUMN_PRICE,
                                ItemEntry.COLUMN_START_ROW, ItemEntry.COLUMN_END_ROW,
                                ItemEntry.COLUMN_TYPE},
                        ItemEntry.COLUMN_RECEIPT_ID + " = ?",
                        new String[]{String.valueOf(receiptId)},
                        null);

            case AUTO_COMPLETE_LOADER:
                return new CursorLoader(this,
                        ReceiptProvider.SHOP_LIST_CONTENT_URI, null, null, null, null);

            default:
                throw new IllegalArgumentException("Unknown loader id");
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LIST_ITEM_LOADER:
                double total = 0;
                data.moveToPosition(-1);
                while (data.moveToNext()) {
                    if (data.getInt(data.getColumnIndex(ItemEntry.COLUMN_TYPE)) == ItemEntry.TYPE_ITEM) {
                        total += data.getDouble(data.getColumnIndex(ItemEntry.COLUMN_PRICE));
                    }
                }
                totalText.setText(String.valueOf(total));
                ContentValues values = new ContentValues();
                values.put(ReceiptEntry.COLUMN_TOTAL, total);
                getContentResolver().update(
                        ContentUris.withAppendedId(ReceiptProvider.RECEIPT_CONTENT_URI, receiptId),
                        values, null, null);
                receiptPreviewAdapter.swapCursor(data);
                break;

            case AUTO_COMPLETE_LOADER:
                simpleCursorAdapter.swapCursor(data);
                break;

            default:
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LIST_ITEM_LOADER:
                totalText.setText("...");
                receiptPreviewAdapter.swapCursor(null);
                break;

            case AUTO_COMPLETE_LOADER:
                simpleCursorAdapter.swapCursor(null);
                break;

            default:
        }
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.button:
                ContentValues values = new ContentValues();
                values.put(ReceiptEntry.COLUMN_SHOP, shopText.getText().toString());
                values.put(ReceiptEntry.COLUMN_DATE, dateText.getText().toString());
                values.put(ReceiptEntry.COLUMN_TOTAL, Double.valueOf(totalText.getText().toString()));
                getContentResolver().update(
                        ContentUris.withAppendedId(ReceiptProvider.RECEIPT_CONTENT_URI, receiptId),
                        values, null, null);
                finish();
                break;

            case R.id.date:
                TextView textView = (TextView) v;
                DialogFragment dialogFragment = new DatePickerDialogFragment();
                Bundle arg = new Bundle();
                arg.putString(DatePickerDialogFragment.ARG_DATE_STRING, textView.getText().toString());
                arg.putInt(DatePickerDialogFragment.ARG_VIEW_ID, R.id.date);
                dialogFragment.setArguments(arg);
                dialogFragment.show(getSupportFragmentManager(), "DatePicker");
                break;

            default:
        }
    }

    @Override
    public void getDateString(int viewId, String date) {
        TextView textView = (TextView) findViewById(viewId);
        textView.setText(date);
    }

    @Override
    public void setView(ImageView imageView, EditText itemName, EditText price, int viewId) {
        Cursor cursor = receiptPreviewAdapter.getCursor();
        cursor.moveToPosition(viewId);

        Range r = new Range(cursor.getInt(cursor.getColumnIndex(ItemEntry.COLUMN_START_ROW)),
                cursor.getInt(cursor.getColumnIndex(ItemEntry.COLUMN_END_ROW)));
        DisplayImageFragment.displayImage(src.rowRange(r), imageView);
        itemName.setText(cursor.getString(cursor.getColumnIndex(ItemEntry.COLUMN_NAME)));
        price.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndex(ItemEntry.COLUMN_PRICE))));
    }

    @Override
    public void onDialogPositiveClick(String itemName, double price, int viewId) {
        Cursor cursor = receiptPreviewAdapter.getCursor();
        cursor.moveToPosition(viewId);
        int itemId = cursor.getInt(cursor.getColumnIndex(ItemEntry._ID));

        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_TYPE, ItemEntry.TYPE_ITEM);
        values.put(ItemEntry.COLUMN_NAME, itemName);
        values.put(ItemEntry.COLUMN_PRICE, price);
        getContentResolver().update(
                ContentUris.withAppendedId(ReceiptProvider.ITEM_CONTENT_URI, itemId),
                values, null, null);
    }


    public static void hideSoftKeyboard(AppCompatActivity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(
                AppCompatActivity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }

    public void setUiToHideKeyboard(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(ReceiptPreviewActivity.this);
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setUiToHideKeyboard(innerView);
            }
        }
    }
}
