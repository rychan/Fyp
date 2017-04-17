package com.example.rychan.fyp.receipt_preview;

import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.rychan.fyp.DatePickerDialogFragment;
import com.example.rychan.fyp.R;
import com.example.rychan.fyp.provider.Contract.*;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;


public class ReceiptPreviewActivity extends AppCompatActivity implements
        View.OnClickListener, AdapterView.OnItemClickListener,
        DatePickerDialogFragment.DialogListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LIST_ITEM_LOADER = 0;
    private static final int AUTO_COMPLETE_LOADER = 1;

    private SimpleCursorAdapter simpleCursorAdapter;
    private ReceiptPreviewAdapter receiptPreviewAdapter;
    private TextView totalText;
    private int receiptId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_preview);

        Intent intent = getIntent();
        receiptId = intent.getIntExtra("receipt_id", -1);
        String shop = intent.getStringExtra("shop");
        String date = intent.getStringExtra("date");
        double total = intent.getDoubleExtra("total", 0);
        String receiptPath = intent.getStringExtra("file");
        Mat src = Imgcodecs.imread(receiptPath, 0);

        AutoCompleteTextView shopText = (AutoCompleteTextView) findViewById(R.id.shop);
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

        TextView dateText = (TextView) findViewById(R.id.date);
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
                        ReceiptProvider.RECEIPT_CONTENT_URI,
                        new String[]{ReceiptEntry._ID, ReceiptEntry.COLUMN_SHOP},
                        null, null, null);

            default:
                throw new IllegalArgumentException("Unknown loader id");
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LIST_ITEM_LOADER:
                double total = 0;
                while (data.moveToNext()) {
                    total += data.getDouble(data.getColumnIndex(ItemEntry.COLUMN_PRICE));
                }
                totalText.setText(String.valueOf(total));
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
}
