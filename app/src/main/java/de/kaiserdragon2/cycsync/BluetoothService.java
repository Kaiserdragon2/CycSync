package de.kaiserdragon2.cycsync;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class BluetoothService {
    private static final String TAG = "BluetoothService";
    private static final String TARGET_NAME = "M2_03E8";
    private static final UUID NORDIC_UART_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("6e400004-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID CHARACTERISTIC_UUIDTX = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID CHARACTERISTIC_UUIDRX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic Characteristic;
    private BluetoothGattCharacteristic rxCharacteristic;
    private BluetoothGattCharacteristic txCharacteristic;
    private boolean notificationSetupComplete = false;
    private boolean txnotificationSetupComplete = false;
    private final Context context;

    public BluetoothService(Context context) {
        this.context = context;
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        } else {
            Log.e(TAG, "BluetoothManager initialization failed");
        }
    }

    public void startScan() {
        BluetoothDevice device = null;
        if (bluetoothAdapter != null) {
            for (BluetoothDevice bondedDevice : bluetoothAdapter.getBondedDevices()) {
                if (bondedDevice.getName().equals(TARGET_NAME)) {
                    device = bondedDevice;
                    connectToDevice(device);
                }
            }
        }
        else {
            if (bluetoothLeScanner == null) {
                Log.e(TAG, "BluetoothLeScanner not initialized");
                return;
            }
            ScanSettings settings = new ScanSettings.Builder().build();
            bluetoothLeScanner.startScan(null, settings, scanCallback);
            Log.i(TAG, "Scan started");
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (TARGET_NAME.equals(device.getName())) {
                Log.i(TAG, "Found target device: " + device.getName() + " - " + device.getAddress());
                connectToDevice(device);
                bluetoothLeScanner.stopScan(scanCallback);
            }
        }

        @Override
        public void onBatchScanResults(java.util.List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "Scan failed with error: " + errorCode);
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                closeGatt();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(NORDIC_UART_SERVICE);
                if (service != null) {
                    rxCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUIDRX);
                    txCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUIDTX);
                    Characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                    if (rxCharacteristic != null && txCharacteristic != null && Characteristic != null) {
                        setCharacteristicNotification(Characteristic, true);

                    } else {
                        Log.e(TAG, "Characteristics not found");
                    }
                } else {
                    Log.e(TAG, "Service not found");
                }
            } else {
                Log.e(TAG, "Service discovery failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            //if (CHARACTERISTIC_UUIDTX.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                handleNotification(data);
            //}
        }


        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Descriptor write successful for " + descriptor.getUuid());
                if (descriptor.getCharacteristic().getUuid().equals(CHARACTERISTIC_UUID)) {
                    setCharacteristicNotification(txCharacteristic, true);
                } else if (descriptor.getCharacteristic().getUuid().equals(CHARACTERISTIC_UUIDTX)) {
                    readDiskspace();
                }
            } else {
                Log.e(TAG, "Descriptor write failed with status: " + status + " for " + descriptor.getUuid());
            }
        }
    };

    private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (bluetoothGatt == null || characteristic == null) return;
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if (descriptor != null) {
            descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        } else {
            Log.e(TAG, "Characteristic CCCD descriptor not found");
        }
    }

    private void handleNotification(byte[] data) {
        // Handle incoming notification data here
        Log.i(TAG, Arrays.toString(data));
    }

    public void disconnect() {
        closeGatt();
    }


    private void closeGatt() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    public void sendCommand(byte[] command, BluetoothGattCharacteristic characteristic) {
        if (characteristic != null && bluetoothGatt != null) {
            characteristic.setValue(command);
            bluetoothGatt.writeCharacteristic(characteristic);
        } else {
            Log.e(TAG, "RX characteristic or BluetoothGatt is null");
        }
    }

    public void readDiskspace() {
        byte[] command = new byte[]{0x09, 0x00, 0x09};
        sendCommand(command, Characteristic);
    }

}
