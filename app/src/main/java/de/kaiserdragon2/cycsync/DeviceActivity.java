package de.kaiserdragon2.cycsync;

import static android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;

import static com.polidea.rxandroidble3.NotificationSetupMode.QUICK_SETUP;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.polidea.rxandroidble3.RxBleClient;
import com.polidea.rxandroidble3.RxBleConnection;
import com.polidea.rxandroidble3.RxBleDevice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DeviceActivity extends AppCompatActivity {
    Context context;
    final String TAG = "DeviceActivity";
    final UUID ServiceUUID = UUID.fromString("6e400004-b5a3-f393-e0a9-e50e24dcca9e");
    final UUID UART_RX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

    final byte[] requestFileList = new byte[]{
            (byte) 0x05, (byte) 0x66, (byte) 0x69, (byte) 0x6c,
            (byte) 0x65, (byte) 0x6c, (byte) 0x69, (byte) 0x73,
            (byte) 0x74, (byte) 0x2e, (byte) 0x74, (byte) 0x78,
            (byte) 0x74, (byte) 0x57
    };


    Button syncButton;

    RxBleClient rxBleClient;
    RxBleConnection Mconnection;
    public static Observable<RxBleConnection> connectionObservable;
    private static final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Disposable connectionDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_connection);
        context = this;
        String selectedDeviceMac = getIntent().getStringExtra("selected_device_mac");


        rxBleClient = RxBleClient.create(this);
        connectAndSetupNotifications(selectedDeviceMac);
        TextView storage = findViewById(R.id.StorageValue);

        syncButton = (findViewById(R.id.button_sync));
        syncButton.setOnClickListener(v -> sync());


    }


    public void sync(){
        file(Mconnection);

    }
    public void connectAndSetupNotifications(String mac) {

        RxBleDevice bleDevice = rxBleClient.getBleDevice(mac);
        connectionObservable = prepareConnectionObservable(bleDevice);
        connectionDisposable = connectionObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnectionReceived, this::onConnectionFailure);
    }

    private Observable<RxBleConnection> prepareConnectionObservable(RxBleDevice bleDevice) {
        return bleDevice
                .establishConnection(false)
                .replay(1)
                .autoConnect();
    }

    private void onConnectionReceived(RxBleConnection connection) {
        Mconnection =connection;
        setupNotificationOnDevice(connection, ServiceUUID);


    }

    private void file(RxBleConnection connection) {
        connection.writeCharacteristic(ServiceUUID, requestFileList)
                .subscribe(bytes -> {
                    // handle the response
                    //String hexString = bytesToHex(bytes);

                   // Log.v(TAG, "Answer" + hexString);
                }, throwable -> {
                    // handle the error
                });
    }

    private void speicher(RxBleConnection connection) {
        byte[] value = new byte[]{(byte) 0x090009};

        connection.writeCharacteristic(ServiceUUID, value)
                .subscribe(bytes -> {
                    // handle the response
                    String hexString = bytesToHex(bytes);

                    Log.v(TAG, "Answer" + hexString);
                }, throwable -> {
                    // handle the error
                });
    }

    private Completable copy(RxBleConnection connection) {
        byte[] value = new byte[]{(byte) 0x43};
        return Completable.fromAction(() -> {
            Thread.sleep(1000);
            Log.v(TAG,"Copy");
            connection.writeCharacteristic(UART_RX, value).blockingGet();
        });
    }

    private Completable processNot(RxBleConnection connection,byte[] bytes ) {
        byte[] value = new byte[]{(byte) 0x43};
        return Completable.fromAction(() -> {
            //Thread.sleep(500);
            //Log.v(TAG, "Bytes:" + Arrays.toString(bytes));
            String hexString = bytesToHex(bytes);
            String ascii = hexToAscii(hexString);
            if (ascii.contains("/")) {
                //  EOT(connection);
            }
            if (ascii.contains("04")) {
                // file(connection);
            }
            if (ascii.contains("filelist.txtT")) {
                //Log.v(TAG, "true");
                copy(connection).subscribeOn(Schedulers.io()).subscribe();
                //Thread.sleep(500);
                //file(connection);
                //Thread.sleep(500);
                //copy(connection).subscribeOn(Schedulers.io()).subscribe();

                //copy(connection);
                //copy(connection);
            }
            if (ascii.contains("filelist.txt 365")) {
                //  writeToFile(bytes);
                //   ack(connection);
                ack(connection);
                copy(connection);
                //speicher(connection);


            }


            Log.v(TAG, "Notification:" + ascii);
            // handle the value change here
        });
    }

    private void ack(RxBleConnection connection) throws InterruptedException {
        byte[] value = new byte[]{(byte) 06};
        //Thread.sleep(1000);
        connection.writeCharacteristic(UART_RX, value).subscribe(bytes -> {
            // handle the response
            String hexString = bytesToHex(bytes);

            Log.v(TAG, "Answer" + hexString);
            //copy(connection);
        }, throwable -> {
            // handle the error
        });
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder output = new StringBuilder("");
        for (byte b : bytes) {
            output.append((String) String.format("%02X", b));
        }
        return output.toString();
    }


    private void onConnectionFailure(Throwable throwable) {
        Log.e(TAG, "Error connecting to device", throwable);
    }


    private void EOT(RxBleConnection connection) {

        byte[] value = new byte[]{(byte) 0xff00ff};
        connection.writeCharacteristic(ServiceUUID, value)
                .subscribe(writebytes -> {
                    // handle the response
                    String hexString = bytesToHex(writebytes);

                    Log.v(TAG, "Answer" + hexString);
                }, throwable -> {
                    // handle the error
                });
    }

    private void setupNotificationOnDevice(RxBleConnection connection, UUID characteristicUUID) {


        connection.setupNotification(characteristicUUID,QUICK_SETUP)
                .doOnNext(notificationObservable -> {
                    // The setup has been successful, now observe value changes.
                    notificationObservable
                            .subscribe(bytes -> {

                                processNot(connection, bytes ).subscribeOn(Schedulers.io()).subscribe();
                                Log.v(TAG,"Notify");

                            }, throwable -> {
                                Log.e(TAG, "Error setting up notification", throwable);
                            });
                })
                .flatMapSingle(notificationObservable -> connection.writeCharacteristic(characteristicUUID, ENABLE_NOTIFICATION_VALUE))
                .subscribe(bytes -> {
                    /*
                    UUID receivedUUID = characteristicUUID;
                     if(receivedUUID.equals(ServiceUUID)) {
                         UUID characteristicsUUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
                         setupNotificationOnDevice(connection,characteristicsUUID);
                         }
                     */

                    Log.d(TAG, "Notification set up successfully");

                    speicher(connection); //works
                    EOT(connection);// works

                    //copy(connection);
                   // ack(connection);
                    //copy(connection);
                    //speicher(connection);

                }, throwable -> {
                    Log.e(TAG, "Error setting up notification", throwable);
                });
    }

    private void setupNotificationOnDevice2(RxBleConnection connection, UUID characteristicUUID) {

        connection.setupNotification(characteristic)
                .doOnNext(notificationObservable -> {
                    // The setup has been successful, now observe value changes.
                    notificationObservable
                            .subscribe(bytes -> {

                                processNot(connection, bytes ).subscribeOn(Schedulers.io()).subscribe();
                                Log.v(TAG,"Notify");

                            }, throwable -> {
                                Log.e(TAG, "Error setting up notification", throwable);
                            });
                })
                .flatMapSingle(notificationObservable -> connection.writeCharacteristic(characteristicUUID, ENABLE_NOTIFICATION_VALUE))
                .subscribe(bytes -> {
                    /*
                    UUID receivedUUID = characteristicUUID;
                     if(receivedUUID.equals(ServiceUUID)) {
                         UUID characteristicsUUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
                         setupNotificationOnDevice(connection,characteristicsUUID);
                         }
                     */

                    Log.d(TAG, "Notification set up successfully");

                    speicher(connection); //works
                    EOT(connection);// works

                    //copy(connection);
                    // ack(connection);
                    //copy(connection);
                    //speicher(connection);

                }, throwable -> {
                    Log.e(TAG, "Error setting up notification", throwable);
                });
    }


    private static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");
        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }

        return output.toString();
    }

    public void writeToFile(byte[] array) {
        String path = (context.getFilesDir() + "/Filelist.txt");
        try {
            File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream stream = new FileOutputStream(path);
            stream.write(array);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectionDisposable != null) {
            connectionDisposable.dispose();
        }
    }


}
