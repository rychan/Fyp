package com.example.rychan.fyp.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by rychan on 17年4月15日.
 */

public final class Contract {

    public Contract(){}

    public static abstract class ReceiptEntry implements BaseColumns {

        public static final String COLUMN_SHOP = "shop";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_TOTAL = "total";
        public static final String COLUMN_FILE = "file";
        public static final String COLUMN_STATUS = "status";
        public static final int STATUS_PROCESSING = 0;
        public static final int STATUS_NEW = 1;
        public static final int STATUS_OLD = 2;
        public static final String[] STATUS = {"Processing", "To be verified", "Old"};

        public static final String DATABASE_TABLE_NAME = "receipt_table";

        public static final String[] ALL_COLUMN = {_ID, COLUMN_SHOP, COLUMN_DATE,
                    COLUMN_TOTAL, COLUMN_FILE, COLUMN_STATUS};
    }

    public static abstract class ItemEntry implements BaseColumns {

        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_PRICE = "price";
        public static final String COLUMN_RECEIPT_ID = "receipt_id";
        public static final String COLUMN_START_ROW = "start_row";
        public static final String COLUMN_END_ROW = "end_row";
        public static final String COLUMN_TYPE = "type";
        public static final int TYPE_ITEM = 0;
        public static final int TYPE_OTHER = 1;

        public static final String DATABASE_TABLE_NAME = "item_table";

        public static final String [] ALL_COLUMN = {_ID, COLUMN_NAME, COLUMN_PRICE,
                COLUMN_RECEIPT_ID,  COLUMN_START_ROW, COLUMN_END_ROW, COLUMN_TYPE,
                ReceiptEntry.COLUMN_SHOP, ReceiptEntry.COLUMN_DATE, ReceiptEntry.COLUMN_TOTAL,
                ReceiptEntry.COLUMN_FILE, ReceiptEntry.COLUMN_STATUS,
                ItemEntry.DATABASE_TABLE_NAME + "." + ItemEntry._ID};
    }

    public static abstract class ReceiptProvider {

        public static final String AUTHORITY = "com.example.rychan.fyp.provider";
        public static final String RECEIPT = "receipt";
        public static final String ITEM = "item";

        public static final Uri RECEIPT_CONTENT_URI = Uri.parse("content://" + AUTHORITY
                + "/" + RECEIPT);
        public static final Uri ITEM_CONTENT_URI = Uri.parse("content://" + AUTHORITY
                + "/" + ITEM);
    }
}
