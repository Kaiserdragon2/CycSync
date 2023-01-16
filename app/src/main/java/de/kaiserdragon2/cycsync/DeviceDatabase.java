package de.kaiserdragon2.cycsync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DeviceDatabase extends SQLiteOpenHelper {
    // Database name and version
    private static final String DATABASE_NAME = "Device.db";
    private static final int DATABASE_VERSION = 1;

    // Table name and column names
    private static final String TABLE_NAME = "devices";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_MAC_ADDRESS = "phone_number";
    private static final String COLUMN_NAME = "selected";
    // SQL statement to create the table
    private static final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + COLUMN_MAC_ADDRESS + " TEXT NOT NULL," + COLUMN_NAME + " TEXT NOT NULL" + ")";


    public DeviceDatabase(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public String getTableName() {
        return TABLE_NAME;
    }

    public String getColumnMac() {
        return COLUMN_MAC_ADDRESS;
    }

    public String getColumnId() {
        return COLUMN_ID;
    }


    public String getColumnSelected() {
        return COLUMN_NAME;
    }



    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the table
        db.execSQL(SQL_CREATE_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop the table if it exists and recreate it
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}