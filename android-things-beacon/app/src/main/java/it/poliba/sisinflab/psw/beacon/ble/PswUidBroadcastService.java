package it.poliba.sisinflab.psw.beacon.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.UUID;

import it.poliba.sisinflab.psw.beacon.R;
import it.poliba.sisinflab.psw.beacon.utils.Utils;

public class PswUidBroadcastService extends Service {

    private static final String TAG = PswUidBroadcastService.class.getSimpleName();
    private static final String SERVICE_UUID = "ae5946d4-e587-4ba8-b6a5-a97cca6affd3";
    private static final UUID CHARACTERISTIC_WEBPAGE_UUID = UUID.fromString(
        "d1a517f0-2499-46ca-9ccc-809bc1c966fa");
    private static final String PREVIOUS_BROADCAST_PSW_INFO_KEY = "previousPswInfo";
    private static final int BROADCASTING_NOTIFICATION_ID = 8;
    private boolean mStartedByRestart;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private NotificationManagerCompat mNotificationManager;
    private String mDisplayInfo;
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mGattServer;
    private byte[] data;
    public static final String TITLE_KEY = "title";
    public static final String RESOURCE_KEY = "res";
    public static final String MAC_KEY = "mac";
    public static final String ONTO_KEY = "onto";
    public static final String INSTANCE_KEY = "id";
    public static final String DATA_KEY = "data";

    private String mac;
    private String onto;
    private String ind;
    private int resource;

    /*
      * Callback handles all incoming requests from GATT clients.
      * From connections to read/write requests.
      */
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        private int transferSpeed = 20;
        private int queueOffset;
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to device " + device.getAddress());
                queueOffset = 0;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from device " + device.getAddress());
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device,
                                                int requestId,
                                                int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.i(TAG, "onCharacteristicReadRequest " + characteristic.getUuid().toString());

            if (CHARACTERISTIC_WEBPAGE_UUID.equals(characteristic.getUuid())) {
                Log.d(TAG, "Data length:" + data.length + ", offset:" + queueOffset);
                if (queueOffset < data.length) {
                    int end = queueOffset + transferSpeed >= data.length ?
                        data.length : queueOffset + transferSpeed;
                    Log.d(TAG, "Data length:" + data.length + ", offset:" + queueOffset + ", end:" + end);
                    mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        Arrays.copyOfRange(data, queueOffset, end));
                    queueOffset = end;
                } else if (queueOffset == data.length) {
                    mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        new byte[]{});
                    queueOffset++;
                }
            }

            /*
             * Unless the characteristic supports WRITE_NO_RESPONSE,
             * always send a response back for any request.
             */
            mGattServer.sendResponse(device,
                requestId,
                BluetoothGatt.GATT_FAILURE,
                0,
                null);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            transferSpeed = mtu - 5;
        }
    };


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        stopSelf();
                        break;
                    default:

                }
            }
        }
    };

    /////////////////////////////////
    // callbacks
    /////////////////////////////////

    @Override
    public void onCreate() {
        super.onCreate();
        mBluetoothLeAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        mNotificationManager = NotificationManagerCompat.from(this);
        mBluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        fetchBroadcastData(intent);
        if (mDisplayInfo == null || data == null) {
            stopSelf();
            return START_STICKY;
        }
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        broadcastPswUid();
        initGattServer();
        return START_STICKY;
    }

    private void fetchBroadcastData(Intent intent) {
        if ((mStartedByRestart = intent == null)) {
            mDisplayInfo = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(PREVIOUS_BROADCAST_PSW_INFO_KEY, null);
            return;
        }
        mDisplayInfo = intent.getStringExtra(TITLE_KEY);

        mac = intent.getStringExtra(MAC_KEY);
        onto = intent.getStringExtra(ONTO_KEY);
        ind = intent.getStringExtra(INSTANCE_KEY);
        resource = intent.getIntExtra(RESOURCE_KEY, -1);
        data = intent.getByteArrayExtra(DATA_KEY);

        if (mac == null || onto == null || ind == null) {
            return;
        }

        if ((data == null || data.length == 0) && resource != -1) {
            try {
                data = Utils.getBytes(getResources().openRawResource(resource));
            } catch (IOException e) {
                data = null;
                Log.e(TAG, "Error reading file");
            }
        } else
            return;

        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putString(PREVIOUS_BROADCAST_PSW_INFO_KEY, mDisplayInfo)
            .apply();
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(stopServiceReceiver);
            unregisterReceiver(mReceiver);
            disableUrlBroadcasting();
        } catch (Exception e) {
            Log.e(TAG, "Error onDestroy()", e);
        }
        super.onDestroy();
    }

    // Fires when user swipes away app from the recent apps list
    @Override
    public void onTaskRemoved (Intent rootIntent) {
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // The callbacks for the ble advertisement events
    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {

        // Fires when the URL is successfully being advertised
        @Override
        public void onStartSuccess(AdvertiseSettings advertiseSettings) {
            Utils.createBroadcastNotification(PswUidBroadcastService.this, stopServiceReceiver,
                BROADCASTING_NOTIFICATION_ID, getString(R.string.psw_uid_notification_title),
                mDisplayInfo, "pswBeaconFilter");
            if (!mStartedByRestart) {
                Toast.makeText(getApplicationContext(), R.string.psw_uid_broadcasting_confirmation,
                    Toast.LENGTH_LONG).show();
            }
        }

        // Fires when the URL could not be advertised
        @Override
        public void onStartFailure(int result) {
            Log.d(TAG, "onStartFailure" + result);
        }
    };


    /////////////////////////////////
    // utilities
    /////////////////////////////////

    // Broadcast via bluetooth the selected OWL annotation
    private void broadcastPswUid() {
        byte[] namespace = null;
        byte[] instance = null;
        try {
            namespace = (onto + ind).getBytes("UTF-8");
            instance = mac.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Could not encode URL", e);
            return;
        }
        AdvertiseData advertiseData = AdvertisePswDataUtils.getPswUidAdvertisementData(namespace, instance);
        AdvertiseSettings advertiseSettings = AdvertiseDataUtils.getAdvertiseSettings(true);
        try {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            mBluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, mAdvertiseCallback);
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), "Beacon Advertising not supported!", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    // Turn off PSW-UID broadcasting
    private void disableUrlBroadcasting() {
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        mNotificationManager.cancel(BROADCASTING_NOTIFICATION_ID);
    }

    private BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSelf();
        }
    };

    private void initGattServer() {
        mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        BluetoothGattService service = new BluetoothGattService(UUID.fromString(SERVICE_UUID),
            BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic webpage = new BluetoothGattCharacteristic(
            CHARACTERISTIC_WEBPAGE_UUID, BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ);
        service.addCharacteristic(webpage);
        mGattServer.addService(service);
    }
}
