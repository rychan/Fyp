package com.example.rychan.fyp.ReceiptPreview;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.rychan.fyp.R;
import com.example.rychan.fyp.provider.Contract.*;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

public class ReceiptPreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_preview);

        Intent intent = getIntent();
        int receiptId = intent.getIntExtra("receipt_id", -1);
        String shop = intent.getStringExtra("shop");
        String date = intent.getStringExtra("date");
        double total = intent.getDoubleExtra("total", 0);
        String receiptPath = intent.getStringExtra("file");
        Mat src = Imgcodecs.imread(receiptPath, 0);

        EditText shopNameText = (EditText) findViewById(R.id.shopName);
        shopNameText.setText(shop);

        EditText dateText = (EditText) findViewById(R.id.date);
        dateText.setText(date);

        TextView totalText = (TextView) findViewById(R.id.total);
        totalText.setText(String.valueOf(total));

        ListView listView = (ListView) findViewById(R.id.list_view);
        Cursor cursor = getContentResolver().query(ReceiptProvider.ITEM_CONTENT_URI,
                null,
                ItemEntry.COLUMN_RECEIPT_ID + " = ?",
                new String[]{String.valueOf(receiptId)},
                null);
        ReceiptPreviewAdapter receiptPreviewAdapter = new ReceiptPreviewAdapter(this, cursor, src);
        listView.setAdapter(receiptPreviewAdapter);

        Button saveButton = (Button) findViewById(R.id.saveButton);
        //saveButton.setOnClickListener(this);
    }

//    @Override
//    public void onClick(View v) {
//        recognitionResult.save(getContentResolver());
//
//        Intent data = new Intent();
//        setResult(RESULT_OK, data);
//        finish();
//    }
}
