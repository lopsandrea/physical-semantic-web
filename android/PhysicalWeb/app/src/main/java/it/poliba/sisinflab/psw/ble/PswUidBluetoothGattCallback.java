package it.poliba.sisinflab.psw.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import org.physical_web.physicalweb.ConnectionListener;
import org.physical_web.physicalweb.Log;
import org.physical_web.physicalweb.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class PswUidBluetoothGattCallback extends BluetoothGattCallback {

    private static final String TAG = PswUidBluetoothGattCallback.class.getSimpleName();
    private static final UUID SERVICE_UUID = UUID.fromString("ae5946d4-e587-4ba8-b6a5-a97cca6affd3");
    private static final UUID CHARACTERISTIC_WEBPAGE_UUID = UUID.fromString(
        "d1a517f0-2499-46ca-9ccc-809bc1c966fa");
    // This is BluetoothGatt.CONNECTION_PRIORITY_HIGH, from API level 21
    private static final int CONNECTION_PRIORITY_HIGH = 1;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic characteristic;
    private ConnectionListener mCallback;
    private int transferRate = 20;
    private boolean running = false;
    private Context context;
    private String mTitle;
    private FileOutputStream mOut;
    private Thread thConnect;

    public PswUidBluetoothGattCallback(Context context) {
        this.context = context;
    }

    public Boolean isRunning() {
        return running;
    }

    public void connect(String deviceAddress, String title) {
        mTitle = title;
        running = true;
        thConnect = new Thread(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)
                    .connectGatt(context, false, PswUidBluetoothGattCallback.this);
            }
        });
        thConnect.start();
    }

    public void connect(String deviceAddress, String title, ConnectionListener callback) {
        mCallback = callback;
        connect(deviceAddress, title);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                     int status) {
        // Make sure the site is running.  It can stop if the dialog is dismissed very quickly.
        if (!isRunning()) {
            close();
            return;
        }

        // Make sure the read was successful.
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.i(TAG, "onCharacteristicRead unsuccessful: " + status);
            close();
            Toast.makeText(context, R.string.ble_download_error_message, Toast.LENGTH_SHORT).show();
            return;
        }

        // Record the data.
        Log.i(TAG, "onCharacteristicRead successful");
        try {
            mOut.write(characteristic.getValue());
        } catch (IOException e) {
            Log.e(TAG, "Could not write to buffer", e);
            close();
            return;
        }

        // Request a new read if we are not done.
        if (characteristic.getValue().length == transferRate) {
            gatt.readCharacteristic(this.characteristic);
            return;
        }

        // At this point we are done.  Show the file.
        Log.i(TAG, "transfer is complete");
        close();
        //openInChrome(getHtmlFile());
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothProfile.STATE_CONNECTED && status == gatt.GATT_SUCCESS) {
            Log.i(TAG, "Connected to GATT server");
            mBluetoothGatt = gatt;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                gatt.requestConnectionPriority(CONNECTION_PRIORITY_HIGH);
                gatt.requestMtu(505);
            } else {
                gatt.discoverServices();
            }
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.i(TAG, "Disconnected to GATT server");
            // ensure progress dialog is removed and running is set false
            close();
        } else if (status != gatt.GATT_SUCCESS) {
            Log.i(TAG, "Status is " + status);
            close();
        }
    }


    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        Log.i(TAG, "MTU changed to " + mtu);
        transferRate = mtu - 5;
        gatt.discoverServices();
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.i(TAG, "Services Discovered");
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "Service discovery failed!");
            return;
        }

        try {
            mOut = new FileOutputStream(getOutputFile());
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found", e);
            return;
        }

        characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(CHARACTERISTIC_WEBPAGE_UUID);
        gatt.readCharacteristic(characteristic);
    }

    private File getOutputFile() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + mTitle + ".owl");
    }

    private void close() {
        if (mCallback != null) {
            mCallback.onConnectionFinished();
        }

        if (mOut != null) {
            try {
                mOut.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close file", e);
                return;
            }
            mOut = null;
        }

        thConnect.interrupt();

        running = false;
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
}
