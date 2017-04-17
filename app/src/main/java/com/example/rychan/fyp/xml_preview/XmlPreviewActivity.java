package com.example.rychan.fyp.xml_preview;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.rychan.fyp.DatePickerDialogFragment;
import com.example.rychan.fyp.R;
import com.example.rychan.fyp.provider.Contract.ItemEntry;
import com.example.rychan.fyp.provider.Contract.ReceiptEntry;
import com.example.rychan.fyp.provider.Contract.ReceiptProvider;

import java.text.SimpleDateFormat;
import java.util.Date;

public class XmlPreviewActivity extends AppCompatActivity implements
        View.OnClickListener, DatePickerDialogFragment.DialogListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LIST_ITEM_LOADER = 0;
    private static final int SHOP_LOADER = 1;

    private SimpleCursorAdapter simpleCursorAdapter;
    private XmlPreviewAdapter xmlPreviewAdapter;
    private TextView startDate;
    private TextView endDate;
    private TextView totalText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xml_preview);

//        Intent intent = getIntent();

        TextView shopText = (TextView) findViewById(R.id.shop);
        shopText.setText("All");
//        simpleCursorAdapter = new SimpleCursorAdapter(this,
//                R.layout.listitem_plaintext,
//                null,
//                new String[]{ReceiptEntry.COLUMN_SHOP},
//                new int[]{R.id.text_view},
//                0);
//        shopText.setAdapter(simpleCursorAdapter);
//        getSupportLoaderManager().initLoader(SHOP_LOADER, null, this);

        startDate = (TextView) findViewById(R.id.start_date);
        startDate.setText("2017-01-01");

        endDate = (TextView) findViewById(R.id.end_date);
        endDate.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

        totalText = (TextView) findViewById(R.id.total);
        totalText.setText(String.valueOf(0));

        ListView listView = (ListView) findViewById(R.id.list_view);
        xmlPreviewAdapter = new XmlPreviewAdapter(this, null);
        listView.setAdapter(xmlPreviewAdapter);
        getSupportLoaderManager().initLoader(LIST_ITEM_LOADER, null, this);

        Button saveButton = (Button) findViewById(R.id.save_button);
        saveButton.setOnClickListener(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LIST_ITEM_LOADER:
                return new CursorLoader(this,
                        ReceiptProvider.ITEM_CONTENT_URI,
                        null,
                        ReceiptEntry.COLUMN_STATUS + " != ? AND " +
                                ReceiptEntry.COLUMN_DATE + " >= ? AND " +
                                ReceiptEntry.COLUMN_DATE + " >= ? ",
                        new String[]{String.valueOf(ReceiptEntry.STATUS_PROCESSING),
                        startDate.getText().toString(), endDate.getText().toString()},
                        ItemEntry.COLUMN_RECEIPT_ID + " ASC ");

//            case SHOP_LOADER:
//                return new CursorLoader(this,
//                        ReceiptProvider.RECEIPT_CONTENT_URI,
//                        new String[]{ReceiptEntry._ID, "DISTINCT " + ReceiptEntry.COLUMN_SHOP},
//                        null, null, null);

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
                xmlPreviewAdapter.swapCursor(data);
                break;
//            case SHOP_LOADER:
//                simpleCursorAdapter.swapCursor(data);
//                break;
            default:
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LIST_ITEM_LOADER:
                totalText.setText("...");
                xmlPreviewAdapter.swapCursor(null);
                break;
//            case SHOP_LOADER:
//                simpleCursorAdapter.swapCursor(null);
//                break;
            default:
        }
    }
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                finish();
                break;

            case R.id.start_date:
            case R.id.end_date:
                TextView textView = (TextView) v;
                DialogFragment dialogFragment = new DatePickerDialogFragment();
                Bundle arg = new Bundle();
                arg.putString(DatePickerDialogFragment.ARG_DATE_STRING, textView.getText().toString());
                arg.putInt(DatePickerDialogFragment.ARG_VIEW_ID, v.getId());
                dialogFragment.setArguments(arg);
                dialogFragment.show(getSupportFragmentManager(), "DatePicker");
                getSupportLoaderManager().restartLoader(LIST_ITEM_LOADER, null, this);
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
