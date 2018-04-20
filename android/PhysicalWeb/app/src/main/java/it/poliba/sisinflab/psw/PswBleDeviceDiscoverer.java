package it.poliba.sisinflab.psw;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.SystemClock;
import android.webkit.URLUtil;

import org.physical_web.collection.EddystoneBeacon;
import org.physical_web.collection.UrlDevice;
import org.physical_web.physicalweb.BleUrlDeviceDiscoverer;
import org.physical_web.physicalweb.Log;
import org.physical_web.physicalweb.R;
import org.physical_web.physicalweb.Utils;
import org.physical_web.physicalweb.ble.ScanRecord;

public class PswBleDeviceDiscoverer extends BleUrlDeviceDiscoverer {

    private static final String TAG = PswBleDeviceDiscoverer.class.getSimpleName();

    public PswBleDeviceDiscoverer(Context context) {
        super(context);
    }

    @Override
    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanBytes) {
        ScanRecord scanRecord = ScanRecord.parseFromBytes(scanBytes);
        if (!leScanMatches(scanRecord)) {
            return;
        }

        byte[] serviceData = scanRecord.getServiceData(EDDYSTONE_URL_SERVICE_UUID);

        // check for PSW beacons
        //if (PswUtils.isPswEnabled(mContext)) {
            if (PswEddystoneBeacon.isPswUrlFrame(serviceData)) {
                EddystoneBeacon beacon = EddystoneBeacon.parseFromServiceData(serviceData, null);
                if (beacon == null || !URLUtil.isNetworkUrl(beacon.getUrl())) {
                    return;
                }
                UrlDevice urlDevice = createUrlDeviceBuilder(TAG + device.getAddress() + beacon.getUrl(),
                    beacon.getUrl())
                    .setTitle(mContext.getString(R.string.psw_url_beacon_title))
                    .setDescription(mContext.getString(R.string.psw_url_description))
                    .setRssi(rssi)
                    .setTxPower(beacon.getTxPowerLevel())
                    .setDeviceType(PswUtils.PSW_URL_DEVICE_TYPE)
                    .build();
                PswUtils.updateRegion(urlDevice);
                reportUrlDevice(urlDevice);
                return;
            } else if (PswEddystoneBeacon.isPswUidFrame(serviceData)) {
                UidEddystoneBeacon beacon = PswEddystoneBeacon.parseUidFromServiceData(serviceData);
                if (beacon == null) {
                    return;
                }
                UrlDevice urlDevice = createUrlDeviceBuilder(TAG + device.getAddress(), device.getAddress())
                    .setTitle(mContext.getString(R.string.psw_uid_beacon_title))
                    .setDescription(mContext.getString(R.string.psw_uid_description))
                    .setDeviceType(PswUtils.PSW_UID_DEVICE_TYPE)
                    .setRssi(rssi)
                    .setTxPower(beacon.getTxPowerLevel())
                    .addExtra(PswDevice.PSW_UID_MAC_KEY, beacon.getRemoteDeviceMac())
                    .addExtra(PswDevice.PSW_UID_ONTO_KEY, beacon.getOntologyID())
                    .addExtra(PswDevice.PSW_UID_INST_KEY, beacon.getInstanceID())
                    .build();
                PswUtils.updateRegion(urlDevice);
                reportUrlDevice(urlDevice);
                return;
            } else
                super.onLeScan(device, rssi, scanBytes);
        /*} else {
            if (PswEddystoneBeacon.isPswUrlFrame(serviceData) || PswEddystoneBeacon.isPswUidFrame(serviceData)) {
                return;
            } else
                super.onLeScan(device, rssi, scanBytes);
        }*/
    }

    private UrlDevice createUidDevice(String id, String mac) {
        UrlDevice urlDevice = createUrlDeviceBuilder(TAG + id, mac).build();
        return (new UrlDevice.Builder(urlDevice)).build();
    }
}
