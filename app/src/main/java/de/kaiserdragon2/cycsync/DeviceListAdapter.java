package de.kaiserdragon2.cycsync;

import static com.polidea.rxandroidble3.internal.logger.LoggerUtil.bytesToHex;
import static de.kaiserdragon2.cycsync.BuildConfig.DEBUG;


import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.recyclerview.widget.RecyclerView;

import com.polidea.rxandroidble3.NotificationSetupMode;
import com.polidea.rxandroidble3.RxBleConnection;
import com.polidea.rxandroidble3.RxBleDevice;
import com.polidea.rxandroidble3.scan.ScanResult;


import java.util.ArrayList;
import java.util.List;

import java.util.UUID;


import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;


class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {

    final String TAG ="DeviceListAdapter";
    DeviceDatabase databaseHelper ;
    private Context context;
    private List<ScanResult> mScanResults;


    private List<ArrayList<String>> mSavedDevices;



    public DeviceListAdapter(List<ScanResult> scanResults, Context context) {
        mScanResults = scanResults;
        this.context = context;
        databaseHelper  = new DeviceDatabase(context);
    }

    public void DeviceListAdapterSaved(Context context) {
        databaseHelper  = new DeviceDatabase(context);
        mSavedDevices = databaseHelper.getAllDevices();
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanResult scanResult = mScanResults.get(position);
        holder.deviceNameTextView.setText(scanResult.getBleDevice().getName());
        holder.deviceAddressTextView.setText(scanResult.getBleDevice().getMacAddress());
    }

    @Override
    public int getItemCount() {
        return mScanResults.size();
    }

    public void updateScanResults(ScanResult scanResult) {
        boolean duplicate = false;
        String newMac = scanResult.getBleDevice().getMacAddress();
        for (ScanResult sr : mScanResults) {
            String currentMac = sr.getBleDevice().getMacAddress();
            if (newMac.equals(currentMac)) {
                duplicate = true;
                break;
            }
        }
        if (!duplicate) {
            mScanResults.add(scanResult);
            notifyDataSetChanged();
        }
    }


    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {


        TextView deviceNameTextView;
        TextView deviceAddressTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            deviceNameTextView = itemView.findViewById(R.id.device_name);
            deviceAddressTextView = itemView.findViewById(R.id.device_address);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            ScanResult scanResult = mScanResults.get(position);
            String deviceName = scanResult.getBleDevice().getName();
            String macAddress = scanResult.getBleDevice().getMacAddress();
            saveSQLData(macAddress,deviceName);
            DeviceListAdapterSaved(context);
            // Do something with the scanResult, for example, connect to it
            //MainActivity.bleDevice = scanResult.getBleDevice();
            //MainActivity.connectionObservable = MainActivity.prepareConnectionObservable();
            //MainActivity.connect2ToDevice(scanResult);
            //MainActivity.setupNotificationOnDevice();
            //MainActivity.writeCMD();
            //connectToDevice(scanResult);
        }




    }
    private void saveSQLData(String mac, String deviceName){


        // Save the data to the database
        ContentValues contentValues = new ContentValues();
        contentValues.put(databaseHelper.getColumnDeviceName(), deviceName);
        contentValues.put(databaseHelper.getColumnMac(), mac);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.insert(databaseHelper.getTableName(), null, contentValues);
        Toast.makeText(context, "Device saved to the database", Toast.LENGTH_SHORT).show();
        }

    }

/*



    private void disconnectFromDevice() {
        if (connectionDisposable != null) {
            connectionDisposable.dispose();
        }
    }

 */






