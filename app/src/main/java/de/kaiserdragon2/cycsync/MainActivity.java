package de.kaiserdragon2.cycsync;

import static de.kaiserdragon2.cycsync.BuildConfig.DEBUG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.polidea.rxandroidble3.RxBleClient;
import com.polidea.rxandroidble3.RxBleConnection;
import com.polidea.rxandroidble3.RxBleDevice;
import com.polidea.rxandroidble3.scan.ScanResult;
import com.polidea.rxandroidble3.scan.ScanSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "MAIN";
    RecyclerView recyclerView;
    private static final PublishSubject<Boolean> disconnectTriggerSubject = PublishSubject.create();
    Disposable scanSubscription;
    public static RxBleDevice bleDevice;
    public static Observable<RxBleConnection> connectionObservable;
    private static final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ArrayList<DeviceInfo> deviceInfoAll = new ArrayList<>();
    static UUID notificationUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    UUID characteristicUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    FloatingActionButton addButton;
    DeviceDatabase databaseHelper;
    private Context context;
    DeviceListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        recyclerView = findViewById(R.id.foundDevices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        addButton = (findViewById(R.id.addButton));
        addButton.setOnClickListener(v -> scan());
        databaseHelper = new DeviceDatabase(context);
        deviceInfoAll = databaseHelper.getAllDevices();
        adapter = new DeviceListAdapter(deviceInfoAll);
        recyclerView.setAdapter(adapter);


    }


    private void scan() {
        RxBleClient rxBleClient = RxBleClient.create(this);
        addButton.setVisibility(View.GONE);
        //adapter = new DeviceListAdapter(deviceInfoAll);
        //recyclerView.setAdapter(adapter);
        updateUI(null);
        scanSubscription = rxBleClient.scanBleDevices(
                        new ScanSettings.Builder().build()
                )
                .filter(scanResult -> Objects.equals(scanResult.getBleDevice().getName(),"M2_03E8"))
                .subscribe(
                        scanResult -> {
                            if (DEBUG) Log.v(TAG, String.valueOf(scanResult));
                            // Process filtered scan result here.
                            updateUI(scanResult);
                        },
                        throwable -> {
                            // Handle an error here.
                        }
                );
        // When done, just dispose.
        //
    }


    private void updateUI(ScanResult scanResult) {
        DeviceListAdapter adapter = (DeviceListAdapter) recyclerView.getAdapter();
        if (adapter != this.adapter && adapter != null) {
            adapter.updateResults(scanResult);
        } else {
            List<DeviceInfo> results = new ArrayList<>();
            DeviceListAdapter newAdapter = new DeviceListAdapter(results);
            recyclerView.setAdapter(newAdapter);

        }
    }


    public static Observable<RxBleConnection> prepareConnectionObservable() {
        return bleDevice
                .establishConnection(true)
                .takeUntil(disconnectTriggerSubject);
        // .compose(ReplayingShare.instance());
    }

    private void connectToDevice(ScanResult scanResult) {
        Disposable disposable = scanResult.getBleDevice().establishConnection(false)
                .flatMap(rxBleConnection -> rxBleConnection.setupNotification(notificationUuid))
                .doOnNext(notificationObservable -> {
                    // Notification has been set up
                })
                .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .subscribe(
                        bytes -> {
                            // Given characteristic has been changes, here is the value.
                        },
                        throwable -> {
                            // Handle an error here.
                        }
                );
        compositeDisposable.add(disposable);
    }

    public static void connect2ToDevice(ScanResult scanResult) {
        Disposable connectionDisposable = connectionObservable
                .flatMapSingle(RxBleConnection::discoverServices)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        rxBleConnection -> {
                            if (DEBUG) Log.v(TAG, String.valueOf(rxBleConnection));

                            // Do something with the connection
                        },
                        throwable -> {
                            if (DEBUG) Log.v(TAG, "Error" + throwable);
                            // Handle connection error
                        }
                );
        compositeDisposable.add(connectionDisposable);
    }

    /*
        public static void setupNotificationOnDevice() {
            Disposable disposable =connectionObservable
                    .flatMap(rxBleService -> rxBleService.setupNotification(notificationUuid, NotificationSetupMode.QUICK_SETUP))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onNotificationReceived, this::onNotificationSetupFailure);
            //.flatMap(rxBleConnection -> rxBleConnection.setupNotification(notificationUuid))

            // <-- Notification has been set up, now observe value changes.

        }

        private void onNotificationReceived(byte[] bytes) {
            //noinspection ConstantConditions
            Log.v( TAG,"Change: " + bytesToHex(bytes));
        }

        private void onNotificationSetupFailure(Throwable throwable) {
            //noinspection ConstantConditions
            Log.v(TAG, "Notifications error: " + throwable);
        }


        public static void writeCMD(){

            byte[] inputBytes = new byte[] {(byte)0x90009};
            final Disposable disposable = connectionObservable
                    .firstOrError()
                    .flatMap(rxBleConnection -> rxBleConnection.writeCharacteristic(characteristicUuid, inputBytes))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bytes -> onWriteSuccess(),
                            this::onWriteFailure
                    );

            compositeDisposable.add(disposable);

        }

        private void onWriteSuccess() {
            //noinspection ConstantConditions
            Log.v(TAG, "Write success");
        }

        private void onWriteFailure(Throwable throwable) {
            //noinspection ConstantConditions
            Log.v(TAG, "Write error: " + throwable);
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
                // Do something with the scanResult, for example, connect to it
                //MainActivity.bleDevice = scanResult.getBleDevice();
                MainActivity.connectionObservable = MainActivity.prepareConnectionObservable();
                MainActivity.connect2ToDevice(scanResult);
                MainActivity.setupNotificationOnDevice();
                MainActivity.writeCMD();
                //connectToDevice(scanResult);
            }

        }

     */
    class DeviceListAdapter extends RecyclerView.Adapter<DeviceViewHolder> {

        private final List<DeviceInfo> mScanResults;


        public DeviceListAdapter(List<DeviceInfo> scanResults) {
            mScanResults = scanResults;

            //notifyDataSetChanged();
        }

        public void DeviceListAdapterSaved(Context context) {
            databaseHelper = new DeviceDatabase(context);
            //mSavedDevices = databaseHelper.getAllDevices();
        }

        private void updateResults(ScanResult scanResult) {
            DeviceInfo deviceInfo = new DeviceInfo(scanResult.getBleDevice().getName(), scanResult.getBleDevice().getMacAddress());
            if (mScanResults.contains(deviceInfo)) return;
            mScanResults.add(deviceInfo);

            notifyItemInserted(mScanResults.size() + 1);
            //notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
            return new DeviceViewHolder(view, mScanResults);
        }

        @Override
        public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
            DeviceInfo deviceInfo = mScanResults.get(position);
            holder.deviceNameTextView.setText(deviceInfo.getDeviceName());
            holder.deviceAddressTextView.setText(deviceInfo.getMacAddress());
        }

        @Override
        public int getItemCount() {
            return mScanResults.size();
        }

        /*
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


         */

    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceNameTextView;
        TextView deviceAddressTextView;

        public DeviceViewHolder(View itemView, List<DeviceInfo> deviceInfo) {
            super(itemView);
            deviceNameTextView = itemView.findViewById(R.id.device_name);
            deviceAddressTextView = itemView.findViewById(R.id.device_address);
            itemView.setOnClickListener(view -> {
                int position = getAdapterPosition();
                Log.v(TAG, String.valueOf(position));

                Log.v(TAG, String.valueOf(deviceInfo));
                DeviceInfo device = deviceInfo.get(position);


                String deviceName = device.getDeviceName();
                String macAddress = device.getMacAddress();
                if (addButton.getVisibility() == View.GONE) {
                    saveSQLData(macAddress, deviceName);
                    addButton.setVisibility(View.VISIBLE);
                    recyclerView.setAdapter(adapter);
                    scanSubscription.dispose();
                } else {
                    Intent intent = new Intent(MainActivity.this, DeviceActivity.class);
                    intent.putExtra("selected_device_mac", macAddress);
                    startActivity(intent);
                }

                //DeviceListAdapterSaved(context);
                // Do something with the scanResult, for example, connect to it
                //MainActivity.bleDevice = scanResult.getBleDevice();
                //MainActivity.connectionObservable = MainActivity.prepareConnectionObservable();
                //MainActivity.connect2ToDevice(scanResult);
                //MainActivity.setupNotificationOnDevice();
                //MainActivity.writeCMD();
                //connectToDevice(scanResult);
            });

        }


    }

    private void saveSQLData(String mac, String deviceName) {
        if (!databaseHelper.checkIfExist(mac)) {
            // Save the data to the database
            ContentValues contentValues = new ContentValues();
            contentValues.put(databaseHelper.getColumnDeviceName(), deviceName);
            contentValues.put(databaseHelper.getColumnMac(), mac);
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            db.insert(databaseHelper.getTableName(), null, contentValues);
            Toast.makeText(context, "Device saved to the database", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(context, "Device already added", Toast.LENGTH_SHORT).show();
    }

}





