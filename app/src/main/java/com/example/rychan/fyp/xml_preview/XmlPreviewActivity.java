package com.example.rychan.fyp.xml_preview;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.rychan.fyp.DatePickerDialogFragment;
import com.example.rychan.fyp.MainActivity;
import com.example.rychan.fyp.R;
import com.example.rychan.fyp.provider.Contract.*;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


import static com.example.rychan.fyp.DatePickerDialogFragment.dateFormat;

public class XmlPreviewActivity extends AppCompatActivity implements
        View.OnClickListener, DatePickerDialogFragment.DialogListener,
        SelectShopDialog.DialogListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LIST_ITEM_LOADER = 0;
    private static final int SHOP_LOADER = 1;

    private XmlPreviewAdapter xmlPreviewAdapter;
    private TextView startDate;
    private TextView endDate;
    private TextView totalText;
    private TextView shopText;

    private ArrayList<String> shopList = new ArrayList<>();
    private ArrayList<String> selectedShopList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xml_preview);

//        Intent intent = getIntent();

        shopText = (TextView) findViewById(R.id.shop);
        shopText.setText("All");
        shopText.setOnClickListener(this);
        getSupportLoaderManager().initLoader(SHOP_LOADER, null, this);

        startDate = (TextView) findViewById(R.id.start_date);
        startDate.setText("2017-01-01");
        startDate.setOnClickListener(this);

        endDate = (TextView) findViewById(R.id.end_date);
        endDate.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        endDate.setOnClickListener(this);

        totalText = (TextView) findViewById(R.id.total);
        totalText.setText("...");

        ListView listView = (ListView) findViewById(R.id.list_view);
        xmlPreviewAdapter = new XmlPreviewAdapter(this, null);
        listView.setAdapter(xmlPreviewAdapter);
        getSupportLoaderManager().initLoader(LIST_ITEM_LOADER, null, this);

        Button saveButton = (Button) findViewById(R.id.button);
        saveButton.setOnClickListener(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LIST_ITEM_LOADER:

                String selection = "";
                for (int i = 0; i < selectedShopList.size(); ++i) {
                    if (i == 0) {
                        selection = selection + "(";
                    }
                    if (i == selectedShopList.size() - 1) {
                        selection = selection + ReceiptEntry.COLUMN_SHOP + " == ? ) AND ";
                    } else {
                        selection = selection + ReceiptEntry.COLUMN_SHOP + " == ? OR ";
                    }
                }
                selection = selection +
                        ItemEntry.COLUMN_TYPE + " == ? AND " +
                        ReceiptEntry.COLUMN_DATE + " >= ? AND " +
                        ReceiptEntry.COLUMN_DATE + " <= ? ";

                List<String> selectionArg = new ArrayList<>(selectedShopList);
                selectionArg.add(String.valueOf(ItemEntry.TYPE_ITEM));
                selectionArg.add(startDate.getText().toString());
                selectionArg.add(endDate.getText().toString());

                return new CursorLoader(this,
                        ReceiptProvider.ITEM_CONTENT_URI,
                        new String[]{ItemEntry.DATABASE_TABLE_NAME + "." + ItemEntry._ID,
                                ItemEntry.COLUMN_NAME, ItemEntry.COLUMN_PRICE,
                                ItemEntry.COLUMN_RECEIPT_ID, ReceiptEntry.COLUMN_SHOP,
                                ReceiptEntry.COLUMN_DATE},
                        selection,
                        selectionArg.toArray(new String[selectionArg.size()]),
                        ItemEntry.COLUMN_RECEIPT_ID + " ASC ");

            case SHOP_LOADER:
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
                    total += data.getDouble(data.getColumnIndex(ItemEntry.COLUMN_PRICE));
                }
                totalText.setText(String.valueOf(total));
                xmlPreviewAdapter.swapCursor(data);
                break;

            case SHOP_LOADER:
                shopList = new ArrayList<>();
                data.moveToPosition(-1);
                while (data.moveToNext()) {
                    shopList.add(data.getString(data.getColumnIndex(FormatEntry.COLUMN_SHOP)));
                }
                data.close();
                break;
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

            case SHOP_LOADER:
                shopList = new ArrayList<>();
                break;
            default:
        }
    }
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                String xmlPath = writeXmlFile();
                if (xmlPath != null) {
                    Intent intent = new Intent();
                    intent.putExtra("xml_path", xmlPath);
                    setResult(RESULT_OK, intent);
                    finish();
                }
                break;

            case R.id.shop:
                DialogFragment selectShopDialog = new SelectShopDialog();
                Bundle arg1 = new Bundle();
                arg1.putStringArrayList(SelectShopDialog.ARG_SHOP_LIST, shopList);
                arg1.putStringArrayList(SelectShopDialog.ARG_SELECTED_SHOP_LIST, selectedShopList);
                selectShopDialog.setArguments(arg1);
                selectShopDialog.show(getSupportFragmentManager(), "ShopSelector");
                break;

            case R.id.start_date:
            case R.id.end_date:
                TextView textView = (TextView) v;
                DialogFragment datePickerDialogFragment = new DatePickerDialogFragment();
                Bundle arg2 = new Bundle();
                arg2.putString(DatePickerDialogFragment.ARG_DATE_STRING, textView.getText().toString());
                arg2.putInt(DatePickerDialogFragment.ARG_VIEW_ID, v.getId());
                datePickerDialogFragment.setArguments(arg2);
                datePickerDialogFragment.show(getSupportFragmentManager(), "DatePicker");
                break;

            default:
        }
    }

    @Override
    public void getDateString(int viewId, String date) {
        try {
            Date newDate = dateFormat.parse(date);
            Date start = dateFormat.parse(startDate.getText().toString());
            Date end = dateFormat.parse(endDate.getText().toString());
            if (viewId == startDate.getId() && !end.before(newDate) ||
                    viewId == endDate.getId() && !start.after(newDate)) {
                TextView textView = (TextView) findViewById(viewId);
                textView.setText(date);
                getSupportLoaderManager().restartLoader(LIST_ITEM_LOADER, null, this);
            }
        } catch (ParseException e) {
        }
    }

    @Override
    public void onDialogPositiveClick(String[] selected_shop) {
        if (selected_shop.length == 0 || selected_shop.length == shopList.size()) {
            shopText.setText("All");
            selectedShopList = new ArrayList<>();
        } else if (selected_shop.length == 1){
            shopText.setText(selected_shop[0]);
            selectedShopList = new ArrayList<>(Arrays.asList(selected_shop));
        } else {
            shopText.setText(String.valueOf(selected_shop.length) + " Shops");
            selectedShopList = new ArrayList<>(Arrays.asList(selected_shop));
        }
        getSupportLoaderManager().restartLoader(LIST_ITEM_LOADER, null, this);
    }

    private String writeXmlFile() {
        File outputFile;
        try {
            Cursor cursor = xmlPreviewAdapter.getCursor();
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS + "/Fyp");
            path.mkdirs();
            outputFile = MainActivity.createFile("Report", ".xml", path);

            FileWriter out = new FileWriter(outputFile);
            XmlSerializer serializer = Xml.newSerializer();
            StringWriter stringWriter = new StringWriter();

            serializer.setOutput(stringWriter);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "report");

            serializer.startTag("", "shop_list");
            serializer.startTag("", "shop");
            serializer.text("All");
            serializer.endTag("", "shop");
            serializer.endTag("", "shop_list");

            serializer.startTag("", "start_date");
            serializer.text(startDate.getText().toString());
            serializer.endTag("", "start_date");

            serializer.startTag("", "end_date");
            serializer.text(endDate.getText().toString());
            serializer.endTag("", "end_date");

            serializer.startTag("", "total");
            serializer.text(totalText.getText().toString());
            serializer.endTag("", "total");

            serializer.startTag("", "item_list");
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                serializer.startTag("", "item");
                for (String s: new String[]{ItemEntry.COLUMN_NAME, ReceiptEntry.COLUMN_SHOP,
                        ReceiptEntry.COLUMN_DATE, ItemEntry.COLUMN_PRICE}) {
                    serializer.startTag("", s);
                    serializer.text(String.valueOf(cursor.getString(cursor.getColumnIndex(s))));
                    serializer.endTag("", s);
                }
                serializer.endTag("", "item");
            }
            serializer.endTag("", "item_list");

            serializer.endTag("", "report");
            serializer.endDocument();

            out.write(stringWriter.toString());
            out.close();

            return outputFile.getAbsolutePath();

        } catch (IOException e) {
            return null;
        }
    }
}
