package de.kaiserdragon2.cycsync;

import static de.kaiserdragon2.cycsync.BuildConfig.DEBUG;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.polidea.rxandroidble3.RxBleClient;
import com.polidea.rxandroidble3.scan.ScanResult;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

public class DeviceDatabase extends SQLiteOpenHelper {
    private final String TAG = "DeviceDatabase";
    // Database name and version
    private static final String DATABASE_NAME = "Device.db";
    private static final int DATABASE_VERSION = 1;

    // Table name and column names
    private static final String TABLE_NAME = "devices";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_MAC_ADDRESS = "mac_address";
    private static final String COLUMN_NAME = "device_name";
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


    public String getColumnDeviceName() {
        return COLUMN_NAME;
    }

    public ArrayList<DeviceInfo> getAllDevices() {
        ArrayList<DeviceInfo> savedDevices = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM devices", null);
        if (cursor.moveToFirst()) {
            do {
                int nameIndex = cursor.getColumnIndex(COLUMN_NAME);
                int macIndex = cursor.getColumnIndex(COLUMN_MAC_ADDRESS);
                if (nameIndex != -1 && macIndex != -1) {
                    String name = cursor.getString(nameIndex);
                    String mac = cursor.getString(macIndex);
                    DeviceInfo deviceInfo =new DeviceInfo(name,mac);
                    savedDevices.add(deviceInfo);
                } else {
                    Log.e(TAG, "Column not found in the cursor");
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return savedDevices;
    }

    public boolean checkIfExist(String mac) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_NAME,
                new String[]{COLUMN_ID},
                COLUMN_MAC_ADDRESS + " = ?",
                new String[]{mac},
                null,
                null,
                null);
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        if(DEBUG)Log.v(TAG,"Device exists = " + exists);
        return exists;
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