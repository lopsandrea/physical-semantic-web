package it.poliba.sisinflab.psw.ble;

import android.annotation.TargetApi;
import android.bluetooth.le.AdvertiseData;

import org.physical_web.physicalweb.ble.AdvertiseDataUtils;

import it.poliba.sisinflab.psw.PswEddystoneBeacon;

public class AdvertisePswDataUtils extends AdvertiseDataUtils {

    // Generate the advertising bytes for the given URL
    @TargetApi(21)
    public static AdvertiseData getPswUidAdvertisementData(byte[] namespace, byte[] instance) {
        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        builder.setIncludeTxPowerLevel(false); // reserve advertising space for URI

        // Manually build the advertising info
        // See https://github.com/google/eddystone/tree/master/eddystone-url
        if (namespace == null || namespace.length == 0 || instance == null || instance.length == 0) {
            return null;
        }

        byte[] beaconData = new byte[20];
        System.arraycopy(namespace, 0, beaconData, 2, 10);
        System.arraycopy(instance, 0, beaconData, 12, 6);
        beaconData[0] = PswEddystoneBeacon.PSW_UID_FRAME_TYPE; // frame type: psw-uid
        beaconData[1] = (byte) 0xBA; // calibrated tx power at 0 m
        beaconData[18] = (byte) 0x00; // Reserved for future use, must be0x00
        beaconData[19] = (byte) 0x00; // Reserved for future use, must be0x00

        builder.addServiceData(EDDYSTONE_BEACON_UUID, beaconData);

        // Adding 0xFEAA to the "Service Complete List UUID 16" (0x3) for iOS compatibility
        builder.addServiceUuid(EDDYSTONE_BEACON_UUID);

        return builder.build();
    }

}
