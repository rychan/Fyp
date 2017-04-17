package com.example.rychan.fyp.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

import static com.example.rychan.fyp.provider.Contract.*;

/**
 * Created by rychan on 17年4月15日.
 */

public class DatabaseProvider extends ContentProvider {
    // Used for the UriMacher
    private static final int RECEIPT = 1;
    private static final int RECEIPT_ID = 2;
    private static final int ITEM = 3;
    private static final int ITEM_ID = 4;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(ReceiptProvider.AUTHORITY, ReceiptProvider.RECEIPT, RECEIPT);
        uriMatcher.addURI(ReceiptProvider.AUTHORITY, ReceiptProvider.RECEIPT + "/#", RECEIPT_ID);
        uriMatcher.addURI(ReceiptProvider.AUTHORITY, ReceiptProvider.ITEM, ITEM);
        uriMatcher.addURI(ReceiptProvider.AUTHORITY, ReceiptProvider.ITEM + "/#", ITEM_ID);
    }

    private DatabaseHelper dbHelper;

    /**
     * Database specific constant declarations
     */
    private SQLiteDatabase db;
    static final String DATABASE_NAME = "receiptData";
    static final int DATABASE_VERSION = 1;
    static final String CREATE_RECEIPT_TABLE =
            " CREATE TABLE " + ReceiptEntry.DATABASE_TABLE_NAME + " (" +
                    ReceiptEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ReceiptEntry.COLUMN_SHOP + " TEXT NOT NULL, " +
                    ReceiptEntry.COLUMN_DATE + " TEXT NOT NULL, " +
                    ReceiptEntry.COLUMN_TOTAL +  " REAL, " +
                    ReceiptEntry.COLUMN_FILE + " TEXT NOT NULL, " +
                    ReceiptEntry.COLUMN_STATUS + " INTEGER);";
    static final String CREATE_ITEM_TABLE =
            " CREATE TABLE " + ItemEntry.DATABASE_TABLE_NAME + " (" +
                    ItemEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ItemEntry.COLUMN_NAME + " TEXT, " +
                    ItemEntry.COLUMN_PRICE + " REAL, " +
                    ItemEntry.COLUMN_RECEIPT_ID + " INTEGER, " +
                    ItemEntry.COLUMN_START_ROW + " INTEGER, " +
                    ItemEntry.COLUMN_END_ROW + " INTEGER, " +
                    ItemEntry.COLUMN_TYPE + " INTEGER);";

    /**
     * Helper class that actually creates and manages
     * the provider's underlying data repository.
     */

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_RECEIPT_TABLE);
            db.execSQL(CREATE_ITEM_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " +  ReceiptEntry.DATABASE_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " +  ItemEntry.DATABASE_TABLE_NAME);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        String joinTable =  ItemEntry.DATABASE_TABLE_NAME + " INNER JOIN " +
                ReceiptEntry.DATABASE_TABLE_NAME + " ON " +
                ReceiptEntry.DATABASE_TABLE_NAME + "." + ReceiptEntry._ID + " = " +
                ItemEntry.DATABASE_TABLE_NAME + "." + ItemEntry.COLUMN_RECEIPT_ID;

        switch (uriMatcher.match(uri)) {
            case RECEIPT:
                queryBuilder.setTables(ReceiptEntry.DATABASE_TABLE_NAME);
                checkColumns(projection, ReceiptEntry.ALL_COLUMN);
                break;
            case RECEIPT_ID:
                queryBuilder.setTables(ReceiptEntry.DATABASE_TABLE_NAME);
                checkColumns(projection, ReceiptEntry.ALL_COLUMN);
                queryBuilder.appendWhere(ReceiptEntry._ID + "=" + uri.getLastPathSegment());
                break;
            case ITEM:
                queryBuilder.setTables(joinTable);
                checkColumns(projection, ItemEntry.ALL_COLUMN);
                break;
            case ITEM_ID:
                queryBuilder.setTables(joinTable);
                checkColumns(projection, ItemEntry.ALL_COLUMN);
                queryBuilder.appendWhere(ItemEntry._ID + "=" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        db = dbHelper.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        // Make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }


    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long id;
        Uri resultUri;
        db = dbHelper.getWritableDatabase();

        switch (uriMatcher.match(uri)) {
            case RECEIPT:
                id = db.insert(ReceiptEntry.DATABASE_TABLE_NAME, null, values);
                resultUri = ReceiptProvider.RECEIPT_CONTENT_URI;
                break;
            case ITEM:
                id = db.insert(ItemEntry.DATABASE_TABLE_NAME, null, values);
                resultUri = ReceiptProvider.ITEM_CONTENT_URI;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        Uri newUri = ContentUris.withAppendedId(resultUri, id);
        getContext().getContentResolver().notifyChange(newUri, null);
        return newUri;
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        db = dbHelper.getWritableDatabase();

        int count = 0;
        switch (uriMatcher.match(uri)){
            case RECEIPT:
                Cursor cursor = query(uri, new String[]{ReceiptEntry._ID}, selection, selectionArgs, null);
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndex(ReceiptEntry._ID));
                    count += delete(ReceiptProvider.ITEM_CONTENT_URI,
                            ItemEntry.COLUMN_RECEIPT_ID + " = ?",
                            new String[]{String.valueOf(id)});
                }
                cursor.close();
                count += db.delete(ReceiptEntry.DATABASE_TABLE_NAME, selection, selectionArgs);
                break;

            case RECEIPT_ID:
                String receiptId = uri.getLastPathSegment();
                count += delete(ReceiptProvider.ITEM_CONTENT_URI,
                        ItemEntry.COLUMN_RECEIPT_ID + " = ?",
                        new String[]{String.valueOf(receiptId)});
                count += db.delete(ReceiptEntry.DATABASE_TABLE_NAME,
                        ReceiptEntry._ID +  " = " + receiptId +
                                (!TextUtils.isEmpty(selection) ? "AND (" + selection + ')' : ""),
                        selectionArgs);
                break;

            case ITEM:
                count = db.delete(ItemEntry.DATABASE_TABLE_NAME, selection, selectionArgs);
                break;

            case ITEM_ID:
                String itemId = uri.getLastPathSegment();
                count = db.delete(ItemEntry.DATABASE_TABLE_NAME,
                        ItemEntry._ID +  " = " + itemId +
                                (!TextUtils.isEmpty(selection) ? "AND (" + selection + ')' : ""),
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        db = dbHelper.getWritableDatabase();
        int count;

        switch (uriMatcher.match(uri)) {
            case RECEIPT:
                count = db.update(ReceiptEntry.DATABASE_TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case RECEIPT_ID:
                String receiptId = uri.getLastPathSegment();
                count = db.update(ReceiptEntry.DATABASE_TABLE_NAME,
                        values,
                        ReceiptEntry._ID +  " = " + receiptId +
                                (!TextUtils.isEmpty(selection) ? "AND (" + selection + ')' : ""),
                        selectionArgs);
                break;
            case ITEM:
                count = db.update(ItemEntry.DATABASE_TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case ITEM_ID:
                String itemId = uri.getLastPathSegment();
                count = db.update(ReceiptEntry.DATABASE_TABLE_NAME,
                        values,
                        ReceiptEntry._ID +  " = " + itemId +
                                (!TextUtils.isEmpty(selection) ? "AND (" + selection + ')' : ""),
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)){
            case RECEIPT:
                return "vnd.android.cursor.dir/vnd.com.example.provider.receipt";
            case RECEIPT_ID:
                return "vnd.android.cursor.item/vnd.com.example.provider.receipt";
            case ITEM:
                return "vnd.android.cursor.dir/vnd.com.example.provider.item";
            case ITEM_ID:
                return "vnd.android.cursor.item/vnd.com.example.provider.item";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    private void checkColumns(String[] projection, String[] available) {
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
            // Check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}
