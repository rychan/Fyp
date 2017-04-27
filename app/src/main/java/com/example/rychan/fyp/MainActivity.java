package com.example.rychan.fyp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.rychan.fyp.receipt_preview.ReceiptPreviewActivity;
import com.example.rychan.fyp.perspective_transform.PerspectiveTransformActivity;
import com.example.rychan.fyp.provider.Contract.*;
import com.example.rychan.fyp.xml_preview.XmlPreviewActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements
        View.OnClickListener, AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, DeleteReceiptDialog.DialogListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "MainActivity";

    private static final int LIST_ITEM_LOADER = 0;
    private SimpleCursorAdapter simpleCursorAdapter;

    private String imagePath;
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int REQUEST_XML_PREVIEW = 3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        simpleCursorAdapter = new SimpleCursorAdapter(
                this,
                R.layout.listitem_main,
                null,
                new String[]{
                        ReceiptEntry.COLUMN_DATE,
                        ReceiptEntry.COLUMN_STATUS,
                        ReceiptEntry.COLUMN_SHOP,
                        ReceiptEntry.COLUMN_TOTAL},
                new int[]{R.id.date, R.id.status, R.id.shop, R.id.total},
                0);
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(simpleCursorAdapter);
        listView.setOnItemClickListener(this);
        listView.setLongClickable(true);
        listView.setOnItemLongClickListener(this);
        getSupportLoaderManager().initLoader(LIST_ITEM_LOADER, null, this);

        Button galleryButton = (Button) findViewById(R.id.gallery_button);
        galleryButton.setOnClickListener(this);
        Button cameraButton = (Button) findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(this);
        Button xmlButton = (Button) findViewById(R.id.export_button);
        xmlButton.setOnClickListener(this);
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "Opencv loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onResume() {
        Log.i(TAG, "called onResume");
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LIST_ITEM_LOADER:
                return new CursorLoader(this,
                        ReceiptProvider.RECEIPT_CONTENT_URI,
                        null, null, null, ReceiptEntry._ID + " DESC");
            default:
                throw new IllegalArgumentException("Unknown loader id");
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LIST_ITEM_LOADER:
                simpleCursorAdapter.swapCursor(data);
            default:
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LIST_ITEM_LOADER:
                simpleCursorAdapter.swapCursor(null);
            default:
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Cursor cursor = simpleCursorAdapter.getCursor();
        if (cursor.moveToPosition(i)) {
            if (cursor.getInt(cursor.getColumnIndex(ReceiptEntry.COLUMN_STATUS)) != ReceiptEntry.STATUS_PROCESSING) {
                int receiptId = cursor.getInt(cursor.getColumnIndex(ReceiptEntry._ID));
                String shop = cursor.getString(cursor.getColumnIndex(ReceiptEntry.COLUMN_SHOP));
                String date = cursor.getString(cursor.getColumnIndex(ReceiptEntry.COLUMN_DATE));
                double total = cursor.getDouble(cursor.getColumnIndex(ReceiptEntry.COLUMN_TOTAL));
                String receiptPath = cursor.getString(cursor.getColumnIndex(ReceiptEntry.COLUMN_FILE));

                dispatchReceiptPreviewIntent(receiptId, shop, date, total, receiptPath);
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        Cursor cursor = simpleCursorAdapter.getCursor();
        if (cursor.moveToPosition(i)) {
            if (cursor.getInt(cursor.getColumnIndex(ReceiptEntry.COLUMN_STATUS)) != ReceiptEntry.STATUS_PROCESSING) {
                DialogFragment dialogFragment = DeleteReceiptDialog.newInstance(
                        cursor.getString(cursor.getColumnIndex(ReceiptEntry.COLUMN_FILE)),
                        cursor.getInt(cursor.getColumnIndex(ReceiptEntry._ID)));
                dialogFragment.show(getSupportFragmentManager(), "delete_receipt");
            }
        }
        return true;
    }

    @Override
    public void onDialogPositiveClick(String filePath, int receiptId) {
        File file = new File(filePath);
        if (!file.delete()) {
            Toast.makeText(this, "Unable to delete binary image file", Toast.LENGTH_LONG).show();
        } else {
            getContentResolver().delete(
                    ContentUris.withAppendedId(ReceiptProvider.RECEIPT_CONTENT_URI, receiptId),
                    null, null);
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.camera_button:
                dispatchCameraIntent();
                break;
            case R.id.gallery_button:
                dispatchGalleryIntent();
                break;
            case R.id.export_button:
                dispatchXmlPreviewIntent();
            default:
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CAMERA:
                if (resultCode == RESULT_OK) {
                    dispatchPerspectiveTransformIntent(imagePath);
                } else if (resultCode == RESULT_CANCELED) {
                    File file = new File(imagePath);
                    file.delete();
                }
                break;

            case REQUEST_GALLERY:
                if (resultCode == RESULT_OK && data != null) {
                    imagePath = getImagePath(data.getData());
                    dispatchPerspectiveTransformIntent(imagePath);
                }
                break;

            case REQUEST_XML_PREVIEW:
                if (resultCode == RESULT_OK && data != null) {
                    dispatchXmlExportIntent(data.getStringExtra("xml_path"));
                }

            default:
        }
    }

    public static File createFile(String prefix, String format, File storageDir) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyymmdd_hhmmss").format(new Date());
        String imageFileName = prefix + "_" + timeStamp + "_";
        return File.createTempFile(imageFileName, format, storageDir);
    }

    private void dispatchCameraIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/Fyp");
                path.mkdirs();
                photoFile = createFile("Photo", ".jpg", path);
                imagePath = photoFile.getAbsolutePath();
            } catch (IOException ex) {
                Log.e(TAG, "Cannot create file");
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(cameraIntent, REQUEST_CAMERA);
            }
        }
    }

    private void dispatchGalleryIntent() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");

        startActivityForResult(galleryIntent, REQUEST_GALLERY);
    }

    private void dispatchPerspectiveTransformIntent(String receiptPath) {
        Intent perspectiveTransformIntent = new Intent(this, PerspectiveTransformActivity.class);
        perspectiveTransformIntent.putExtra("receipt_path", receiptPath);

        startActivity(perspectiveTransformIntent);
    }

    private void dispatchReceiptPreviewIntent(int receiptId, String shop, String date, double total, String receiptPath) {
        Intent receiptPreviewIntent = new Intent(this, ReceiptPreviewActivity.class);
        receiptPreviewIntent.putExtra("receipt_id", receiptId);
        receiptPreviewIntent.putExtra("shop", shop);
        receiptPreviewIntent.putExtra("date", date);
        receiptPreviewIntent.putExtra("total", total);
        receiptPreviewIntent.putExtra("file", receiptPath);

        ContentValues values = new ContentValues();
        values.put(ReceiptEntry.COLUMN_STATUS, ReceiptEntry.STATUS_OLD);
        getContentResolver().update(
                ContentUris.withAppendedId(ReceiptProvider.RECEIPT_CONTENT_URI, receiptId),
                values, null, null);

        startActivity(receiptPreviewIntent);
    }

    private void dispatchXmlPreviewIntent() {
        Intent xmlPreviewIntent = new Intent(this, XmlPreviewActivity.class);

        startActivityForResult(xmlPreviewIntent, REQUEST_XML_PREVIEW);
    }

    private void dispatchXmlExportIntent(String xmlPath) {
        Intent xmlExportIntent = new Intent(Intent.ACTION_SEND);
        xmlExportIntent.setType("application/xml");
        xmlExportIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + xmlPath));

        startActivity(xmlExportIntent);
    }

    public String getImagePath(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
        cursor.close();

        cursor = getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        // Save state members to saved instance
        savedInstanceState.putString("photo_path", imagePath);

        // Always call the superclass so it can save the view hierarchy
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance
        imagePath = savedInstanceState.getString("photo_path");
    }
}