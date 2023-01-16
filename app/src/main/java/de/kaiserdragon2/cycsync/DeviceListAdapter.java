package de.kaiserdragon2.cycsync;

import static com.polidea.rxandroidble3.internal.logger.LoggerUtil.bytesToHex;
import static de.kaiserdragon2.cycsync.BuildConfig.DEBUG;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.recyclerview.widget.RecyclerView;

import com.polidea.rxandroidble3.NotificationSetupMode;
import com.polidea.rxandroidble3.RxBleConnection;
import com.polidea.rxandroidble3.RxBleDevice;
import com.polidea.rxandroidble3.scan.ScanResult;


import java.util.List;

import java.util.UUID;


import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;


class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {

    final String TAG ="DeviceListAdapter";

    private List<ScanResult> mScanResults;


    public DeviceListAdapter(List<ScanResult> scanResults) {
        mScanResults = scanResults;
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
        // check if the scan result is already in the list
        if (!mScanResults.contains(scanResult.getBleDevice().getName())) {
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
            // Do something with the scanResult, for example, connect to it
            //MainActivity.bleDevice = scanResult.getBleDevice();
            //MainActivity.connectionObservable = MainActivity.prepareConnectionObservable();
            //MainActivity.connect2ToDevice(scanResult);
            //MainActivity.setupNotificationOnDevice();
            //MainActivity.writeCMD();
            //connectToDevice(scanResult);
        }




    }

/*



    private void disconnectFromDevice() {
        if (connectionDisposable != null) {
            connectionDisposable.dispose();
        }
    }

 */


    }


