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
        public static final String DATABASE_TABLE_NAME = "receiptTable";
    }

    public static abstract class ItemEntry implements BaseColumns {

        public static final String COLUMN_ITEM = "item";
        public static final String COLUMN_PRICE = "price";
        public static final String COLUMN_RECEIPT_ID = "receiptId";
        public static final String DATABASE_TABLE_NAME = "itemTable";
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
