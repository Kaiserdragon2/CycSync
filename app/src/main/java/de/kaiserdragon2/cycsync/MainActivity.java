package de.kaiserdragon2.cycsync;

import static com.polidea.rxandroidble3.internal.logger.LoggerUtil.bytesToHex;
import static de.kaiserdragon2.cycsync.BuildConfig.DEBUG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.polidea.rxandroidble3.NotificationSetupMode;
import com.polidea.rxandroidble3.RxBleClient;
import com.polidea.rxandroidble3.RxBleConnection;
import com.polidea.rxandroidble3.RxBleDevice;
import com.polidea.rxandroidble3.internal.RxBleDeviceImpl_Factory;
import com.polidea.rxandroidble3.scan.ScanResult;
import com.polidea.rxandroidble3.scan.ScanSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
public class MainActivity extends AppCompatActivity {
    private RxBleClient rxBleClient;
    static final String TAG = "MAIN";
    RecyclerView recyclerView;
    private static PublishSubject<Boolean> disconnectTriggerSubject = PublishSubject.create();
    public static RxBleDevice bleDevice;
    public static Observable<RxBleConnection> connectionObservable;
    private static final CompositeDisposable compositeDisposable = new CompositeDisposable();
    static UUID notificationUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    UUID characteristicUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    FloatingActionButton addButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rxBleClient = RxBleClient.create(this);
        recyclerView = findViewById(R.id.foundDevices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        addButton =(findViewById(R.id.addButton));
        addButton.setOnClickListener(v -> scan());

    }


    private void scan(){
        addButton.setVisibility(View.GONE);
        Disposable scanSubscription = rxBleClient.scanBleDevices(
                        new ScanSettings.Builder().build()
                )
                .filter(scanResult -> scanResult.getBleDevice().getName().equals("M2_03E8"))
                .subscribe(
                        scanResult -> {
                            Log.v(TAG, String.valueOf(scanResult));
                            // Process filtered scan result here.
                            updateUI(scanResult);
                        },
                        throwable -> {
                            // Handle an error here.
                        }
                );
        // When done, just dispose.
        //scanSubscription.dispose();
    }



    private void updateUI(ScanResult scanResult) {
        DeviceListAdapter adapter = (DeviceListAdapter) recyclerView.getAdapter();
        if (adapter != null) {
            adapter.updateScanResults(scanResult);
        } else {
            List<ScanResult> results = new ArrayList<>();
            results.add(scanResult);
            DeviceListAdapter newAdapter = new DeviceListAdapter(results,this);
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
                            if(DEBUG) Log.v(TAG, String.valueOf(rxBleConnection));

                            // Do something with the connection
                        },
                        throwable -> {
                            if(DEBUG) Log.v(TAG, "Error" + throwable);
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
}
